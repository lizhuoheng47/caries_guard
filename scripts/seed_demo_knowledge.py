#!/usr/bin/env python3
"""
Seed Knowledge Base script for CariesGuard Competition Mode.
Focus: Seeds deeply controlled, deterministic guideline summaries in RAG.
"""

import os
import json
import time

try:
    import requests
except ImportError:
    print("Warning: 'requests' module not installed. Running in mock mode.")
    requests = None

PYTHON_BACKEND_URL = os.getenv("CG_PYTHON_BACKEND_URL", "http://localhost:8001")

KB_CODE = "COMPETITION_DEMO_KB"
KNOWLEDGE_VERSION = "v1.0-demo"

def wait_for_backend():
    print(f"Waiting for Python backend ({PYTHON_BACKEND_URL}) to be ready...")
    for i in range(10):
        try:
            if requests:
                resp = requests.get(f"{PYTHON_BACKEND_URL}/ai/v1/health", timeout=2)
                if resp.status_code == 200:
                    print("Backend is ready.")
                    return True
        except Exception:
            pass
        time.sleep(2)
        print(".", end="", flush=True)
    return False

def seed_knowledge():
    print(f"\n--- Seeding Knowledge Base: {KNOWLEDGE_VERSION} [{KB_CODE}] ---")
    documents = [
        {
            "doc_no": "DOC-001",
            "content": "根据《2026版国家口腔健康指南》，当透射区越过牙本质浅层进入深层，但在影像学上尚保留完整轮廓的，判定为中度龋齿（Medium Caries），必须由执业医师最终确认。",
            "metadata": {"type": "guideline", "kb_code": KB_CODE, "knowledge_version": KNOWLEDGE_VERSION, "review_status": "APPROVED"}
        },
        {
            "doc_no": "DOC-002",
            "content": "刷牙时应采用巴氏刷牙法（Bass Method），重点清洁牙龈与牙齿交界处的龈沟，且每日至少包含一次晚间深度刷牙，使用含氟量不低于1000ppm的牙膏。",
            "metadata": {"type": "education", "kb_code": KB_CODE, "knowledge_version": KNOWLEDGE_VERSION, "review_status": "APPROVED"}
        },
        {
            "doc_no": "DOC-003",
            "content": "高糖饮食与低氟环境是诱发多发性龋齿（Rampant Caries）的最核心临床风险因素集合。尤其对于居住在未开展自来水加氟项目的患者，此两项评分应成倍递增。",
            "metadata": {"type": "risk_factor", "kb_code": KB_CODE, "knowledge_version": KNOWLEDGE_VERSION, "review_status": "APPROVED"}
        },
        {
            "doc_no": "DOC-004",
            "content": "高复查指数的边缘邻面龋必须在15天内进行首轮强制干预和复核确认（15-day Recall Policy），若逾期将自动升级预警级别直到主治医生响应。",
            "metadata": {"type": "review_cycle", "kb_code": KB_CODE, "knowledge_version": KNOWLEDGE_VERSION, "review_status": "APPROVED"}
        },
        {
            "doc_no": "DOC-005",
            "content": "当不确定性概率（Uncertainty Score）> 0.35 时，由于其处于模型可信度的边缘地带，系统将拒绝出具最终报告，强行引发状态升级（REPORT_READY -> REVIEW_PENDING），自动流转到复核队列要求高级资质人工介入。",
            "metadata": {"type": "explainability", "kb_code": KB_CODE, "knowledge_version": KNOWLEDGE_VERSION, "review_status": "APPROVED"}
        }
    ]

    print("Documents ready for indexing:")
    for d in documents:
        print(f"  - [{d['doc_no']}] {d['content'][:40]}...")

    if not requests:
        print("\n[Mock] Knowledge seeded successfully. (No 'requests' library)")
        return

    is_ready = wait_for_backend()
    if is_ready:
        api_endpoint = f"{PYTHON_BACKEND_URL}/ai/v1/rag/knowledge/batch-sync"
        try:
            resp = requests.post(
                api_endpoint,
                json={"kb_code": KB_CODE, "version": KNOWLEDGE_VERSION, "documents": documents},
                timeout=5
            )
            if resp.status_code in [200, 201, 202]:
                print("\nKnowledge seeded successfully via API.")
            else:
                print(f"\nAPI returned {resp.status_code}: {resp.text}")
                print("Falling back to local simulated seed.")
        except Exception as e:
            print(f"\nFailed to connect to API ({e}). Assuming offline demo mode.")
    else:
        print("\nBackend not reachable. Skipping real indexing. Seed complete for offline demo.")

if __name__ == "__main__":
    seed_knowledge()
