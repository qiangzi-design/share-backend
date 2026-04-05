#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
每日 AI 快讯抓取脚本：
1) 聚合多个公开数据源（RSS + API）；
2) 统一字段结构并做标题去重；
3) 计算热度分后输出 TopN JSON 给 Java 服务落库。
"""

from __future__ import annotations

import argparse
import html
import json
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Iterable, List

"""
强制脚本标准输出为 UTF-8：
1) Windows 下默认控制台编码可能是 GBK；
2) Java 端固定按 UTF-8 读取时会出现乱码；
3) 在脚本入口提前重设 stdout/stderr 编码，确保跨平台一致。
"""
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


@dataclass
class HotItem:
    """热点条目模型（脚本内部结构）。"""

    title: str
    summary: str
    source_name: str
    source_url: str
    event_time: datetime | None
    raw_score: float


def parse_args() -> argparse.Namespace:
    """解析命令行参数，支持指定目标日期和返回条目数。"""
    parser = argparse.ArgumentParser(description="Fetch daily AI hot topics")
    parser.add_argument("--date", default=datetime.now().date().isoformat(), help="目标日期，格式 YYYY-MM-DD")
    parser.add_argument("--limit", type=int, default=8, help="返回条目数量上限")
    return parser.parse_args()


def http_get_json(url: str, timeout: int = 12, extra_headers: dict | None = None) -> dict:
    """发起 HTTP GET 并解析 JSON。"""
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
    }
    if extra_headers:
        headers.update(extra_headers)
    req = urllib.request.Request(
        url,
        headers=headers,
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        # 按响应头声明字符集解码，减少国内站点 GBK/GB2312 页面乱码风险。
        charset = resp.headers.get_content_charset() or "utf-8"
        raw = resp.read().decode(charset, errors="ignore")
    return json.loads(raw)


def http_get_text(url: str, timeout: int = 12, extra_headers: dict | None = None) -> str:
    """发起 HTTP GET 并返回文本。"""
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    }
    if extra_headers:
        headers.update(extra_headers)
    req = urllib.request.Request(
        url,
        headers=headers,
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        charset = resp.headers.get_content_charset() or "utf-8"
        return resp.read().decode(charset, errors="ignore")


def clean_text(value: str | None, max_len: int = 280) -> str:
    """清理 HTML/空白字符并做长度截断。"""
    if not value:
        return ""
    text = re.sub(r"<[^>]+>", " ", value)
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > max_len:
        return text[: max_len - 3] + "..."
    return text


def parse_datetime(value: str | None) -> datetime | None:
    """兼容多种时间格式（主要用于字符串时间）。"""
    if not value:
        return None
    value = value.strip()
    if not value:
        return None

    # ISO8601
    try:
        if value.endswith("Z"):
            value = value[:-1] + "+00:00"
        dt = datetime.fromisoformat(value)
        if dt.tzinfo is None:
            return dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc)
    except Exception:
        return None


def parse_hot_number(text: str | None) -> float:
    """从“792 万热度”这类文本中提取数值，提取失败返回 0。"""
    if not text:
        return 0.0
    value = text.strip()
    matched = re.search(r"([0-9]+(?:\.[0-9]+)?)", value)
    if not matched:
        return 0.0
    number = float(matched.group(1))
    if "亿" in value:
        return number * 100000000.0
    if "万" in value:
        return number * 10000.0
    return number


def fetch_zhihu_hot_items(weight: float) -> List[HotItem]:
    """抓取知乎热榜（移动端公开接口）。"""
    url = "https://api.zhihu.com/topstory/hot-list"
    payload = http_get_json(url, extra_headers={"Referer": "https://www.zhihu.com/"})
    rows = payload.get("data") or []
    items: List[HotItem] = []

    for index, row in enumerate(rows, start=1):
        target = row.get("target") or {}
        title = clean_text(target.get("title"), 220)
        if not title:
            continue
        summary = clean_text(target.get("excerpt"), 180)
        if not summary:
            summary = clean_text(row.get("detail_text"), 180)

        detail_text = row.get("detail_text") or ""
        hot_value = parse_hot_number(detail_text)
        created_ts = target.get("created")
        event_time = None
        if isinstance(created_ts, (int, float)) and created_ts > 0:
            event_time = datetime.fromtimestamp(float(created_ts), tz=timezone.utc)

        source_url = target.get("url") or ""
        if not source_url and target.get("id"):
            source_url = f"https://www.zhihu.com/question/{target.get('id')}"

        # 热度分由“榜位 + 公开热度文本”联合构成，保证可排序且稳定。
        score = weight + max(0.0, 40 - index * 0.7) + min(hot_value / 100000.0, 35.0)
        items.append(
            HotItem(
                title=title,
                summary=summary or "知乎热榜话题",
                source_name="知乎热榜",
                source_url=source_url or "https://www.zhihu.com/hot",
                event_time=event_time,
                raw_score=score,
            )
        )
    return items


def fetch_toutiao_hot_items(weight: float) -> List[HotItem]:
    """抓取今日头条热榜（PC 热点接口）。"""
    url = "https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc"
    payload = http_get_json(url, extra_headers={"Referer": "https://www.toutiao.com/"})
    rows = payload.get("data") or []
    items: List[HotItem] = []
    for index, row in enumerate(rows, start=1):
        title = clean_text(row.get("Title"), 220)
        if not title:
            continue
        summary = clean_text(row.get("LabelDesc"), 180) or "头条热榜话题"
        hot_value = float(row.get("HotValue") or 0)
        score = weight + max(0.0, 36 - index * 0.6) + min(hot_value / 800000.0, 40.0)
        items.append(
            HotItem(
                title=title,
                summary=summary,
                source_name="今日头条热榜",
                source_url=clean_text(row.get("Url"), 600) or "https://www.toutiao.com/hot-event/hot-board/",
                event_time=None,
                raw_score=score,
            )
        )
    return items


def fetch_weibo_hot_items(weight: float) -> List[HotItem]:
    """抓取微博热榜（weibo.cn 公开移动页，规避主站 403 限制）。"""
    url = "https://weibo.cn/pub/?vt=4"
    text = http_get_text(url, extra_headers={"Referer": "https://weibo.cn/"})

    # 该页面里热榜词条是指向 m.weibo.cn/search 的链接。
    pattern = re.compile(r'<a href="(https://m\.weibo\.cn/search\?containerid[^"]+)"[^>]*>([^<]{2,120})</a>')
    matches = pattern.findall(text)
    items: List[HotItem] = []
    seen_titles: set[str] = set()

    for index, (raw_link, raw_title) in enumerate(matches, start=1):
        title = clean_text(html.unescape(raw_title), 220)
        if not title or title in seen_titles:
            continue
        seen_titles.add(title)
        source_url = html.unescape(raw_link).replace("&amp;", "&")
        score = weight + max(0.0, 35 - index * 0.8)
        items.append(
            HotItem(
                title=title,
                summary="微博热搜话题",
                source_name="微博热搜",
                source_url=source_url,
                event_time=None,
                raw_score=score,
            )
        )
        if len(items) >= 50:
            break
    return items


def deduplicate(items: Iterable[HotItem]) -> List[HotItem]:
    """按标题与链接去重，避免多源重复刷屏。"""
    result: List[HotItem] = []
    seen: set[str] = set()

    for item in items:
        title_key = re.sub(r"\W+", "", item.title.lower())
        url_key = (item.source_url or "").strip().lower()
        key = f"{title_key}|{url_key}"
        if key in seen:
            continue
        seen.add(key)
        result.append(item)
    return result


def is_ai_related(item: HotItem) -> bool:
    """仅保留 AI 相关条目，避免泛新闻混入每日快讯。"""
    text = f"{item.title} {item.summary}".lower()

    # 英文关键词采用边界匹配，避免把普通词中的“ai”子串误判为 AI。
    english_patterns = [
        r"(^|[^a-z])ai([^a-z]|$)",
        r"artificial intelligence",
        r"llm",
        r"gpt[-\s]?[0-9a-z]*",
        r"openai",
        r"anthropic",
        r"claude",
        r"gemini",
        r"deepseek",
        r"agent",
        r"diffusion",
        r"machine learning",
    ]
    for pattern in english_patterns:
        if re.search(pattern, text):
            return True

    chinese_keywords = [
        "机器学习",
        "大模型",
        "生成式",
        "智能体",
        "推理模型",
        "多模态",
        "人工智能",
        "模型训练",
        "算力",
        "aigc",
    ]
    return any(word in text for word in chinese_keywords)


def score_item(item: HotItem, now_utc: datetime) -> float:
    """计算最终热度分：基础分 + 时效性 + 关键词加权。"""
    score = float(item.raw_score)

    # 时效加权：72 小时内越新越高。
    if item.event_time is not None:
        hours = max((now_utc - item.event_time).total_seconds() / 3600.0, 0)
        if hours <= 72:
            score += (72 - hours) / 72 * 30

    # 关键词加权：对“模型发布/融资/推理/开源”等事件略微加分。
    keywords = ["model", "gpt", "llm", "open", "funding", "agent", "release", "openai", "推理", "开源", "发布"]
    title_lower = item.title.lower()
    summary_lower = item.summary.lower()
    hit_count = sum(1 for word in keywords if word in title_lower or word in summary_lower)
    score += min(hit_count * 2.5, 12)

    return round(score, 2)


def build_result(date_text: str, limit: int) -> dict:
    """聚合多源数据并构建输出结果。"""
    now_utc = datetime.now(timezone.utc)
    all_items: List[HotItem] = []
    # 国内数据源组合：
    # 1) 知乎热榜（问题讨论热度）；
    # 2) 头条热榜（全网事件热度）；
    # 3) 微博热搜（实时话题传播热度）。
    sources = [
        ("zhihu", lambda: fetch_zhihu_hot_items(58.0)),
        ("toutiao", lambda: fetch_toutiao_hot_items(56.0)),
        ("weibo", lambda: fetch_weibo_hot_items(54.0)),
    ]

    for _, fetcher in sources:
        try:
            items = fetcher()
            if items:
                all_items.extend(items)
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, json.JSONDecodeError):
            # 单个来源失败不影响整体，继续抓取其他来源。
            continue
        except Exception:
            continue

    deduped_all = deduplicate(all_items)
    deduped_ai = [item for item in deduped_all if is_ai_related(item)]
    if not deduped_all:
        raise RuntimeError("所有数据源均不可用，未获取到热点数据")
    # 若当天国内平台 AI 关键词不足，则回退为综合热点，避免整日无快讯。
    use_fallback = len(deduped_ai) == 0
    deduped = deduped_ai if deduped_ai else deduped_all

    scored = []
    for item in deduped:
        scored.append((score_item(item, now_utc), item))

    scored.sort(key=lambda x: x[0], reverse=True)
    top_items = scored[: max(1, min(limit, 20))]

    payload_items = []
    for idx, (score, item) in enumerate(top_items, start=1):
        payload_items.append(
            {
                "rank": idx,
                "score": score,
                "title": clean_text(item.title, 300),
                "summary": clean_text(item.summary, 2000),
                "sourceName": clean_text(item.source_name, 120),
                "sourceUrl": clean_text(item.source_url, 600),
                "eventTime": item.event_time.astimezone().strftime("%Y-%m-%d %H:%M:%S") if item.event_time else None,
            }
        )

    title = f"每日AI快讯（{date_text}）"
    if use_fallback:
        summary = f"今日国内平台 AI 关键词热点不足，已回退展示 {len(payload_items)} 条综合热点。"
    else:
        summary = f"今日共汇总 {len(payload_items)} 条AI热点，来源覆盖知乎、头条、微博等国内平台。"
    source_count = len({item.source_name for item in deduped_all})

    return {
        "date": date_text,
        "title": title,
        "summary": summary,
        "sourceCount": source_count,
        "items": payload_items,
    }


def main() -> int:
    """脚本入口：输出 JSON，异常时返回非0退出码。"""
    args = parse_args()
    try:
        # 日期参数仅用于标题展示与后端幂等写入，不参与抓取过滤。
        datetime.strptime(args.date, "%Y-%m-%d")
    except ValueError:
        print(json.dumps({"error": "date 参数格式错误，应为 YYYY-MM-DD"}, ensure_ascii=False))
        return 2

    try:
        result = build_result(args.date, args.limit)
        print(json.dumps(result, ensure_ascii=False))
        return 0
    except Exception as ex:
        print(json.dumps({"error": str(ex)}, ensure_ascii=False))
        return 3


if __name__ == "__main__":
    sys.exit(main())
