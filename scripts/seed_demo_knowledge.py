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
JAVA_BACKEND_URL = os.getenv("CARIES_JAVA_BACKEND_URL", "http://localhost:8080")

KB_CODE = "caries-default"
KNOWLEDGE_VERSION = "v1.0-demo"
ORG_ID = 100001

def wait_for_backend():
    print(f"Waiting for Python ({PYTHON_BACKEND_URL}) and Java ({JAVA_BACKEND_URL}) backends to be ready...")
    for i in range(15):
        try:
            if requests:
                py_resp = requests.get(f"{PYTHON_BACKEND_URL}/ai/v1/health", timeout=2)
                ja_resp = requests.get(f"{JAVA_BACKEND_URL}/actuator/health", timeout=2)
                if py_resp.status_code == 200 and ja_resp.status_code == 200:
                    print("\nBoth Backends are ready.")
                    return True
        except Exception:
            pass
        time.sleep(2)
        print(".", end="", flush=True)
    print("\nTimeout waiting for backends.")
    return False

def seed_knowledge():
    print(f"\n--- Seeding Knowledge Base: {KNOWLEDGE_VERSION} [{KB_CODE}] ---")
    documents = [
        {
            "kbCode": KB_CODE,
            "kbName": "CariesGuard Competition Demo Knowledge",
            "kbTypeCode": "DOCTOR_GUIDE",
            "docTitle": "《2026版国家口腔健康指南》",
            "docSourceCode": "DEMO-DOC-001",
            "sourceUri": "demo://doc/001",
            "docVersion": KNOWLEDGE_VERSION,
            "reviewStatusCode": "APPROVED",
            "orgId": ORG_ID,
            "contentText": "根据《2026版国家口腔健康指南》，当透射区越过牙本质浅层进入深层，但在影像学上尚保留完整轮廓的，判定为中度龋齿（Medium Caries），若达到深龋或不确定性高则必须由执业医师最终确认。"
        },
        {
            "kbCode": KB_CODE,
            "kbName": "CariesGuard Competition Demo Knowledge",
            "kbTypeCode": "PATIENT_GUIDE",
            "docTitle": "预防龋齿患者教育手册",
            "docSourceCode": "DEMO-DOC-002",
            "sourceUri": "demo://doc/002",
            "docVersion": KNOWLEDGE_VERSION,
            "reviewStatusCode": "APPROVED",
            "orgId": ORG_ID,
            "contentText": "刷牙时应采用巴氏刷牙法，重点清洁牙龈与牙齿交界处的龈沟，每日至少一次晚间深度刷牙，使用含氟量1000ppm牙膏。中度龋齿患者应重点注意减少睡前饮食。"
        },
        {
            "kbCode": KB_CODE,
            "kbName": "CariesGuard Competition Demo Knowledge",
            "kbTypeCode": "RISK_GUIDE",
            "docTitle": "高糖饮食与低氟环境风险因子定义",
            "docSourceCode": "DEMO-DOC-003",
            "sourceUri": "demo://doc/003",
            "docVersion": KNOWLEDGE_VERSION,
            "reviewStatusCode": "APPROVED",
            "orgId": ORG_ID,
            "contentText": "高糖饮食与低氟环境是诱发多发性龋齿的最核心临床风险因素。系统在风险融合评估时，当患者具有高频糖摄入史且使用无氟水源，其龋齿进展风险分自动调整为 HIGH。"
        },
        {
            "kbCode": KB_CODE,
            "kbName": "CariesGuard Competition Demo Knowledge",
            "kbTypeCode": "DOCTOR_GUIDE",
            "docTitle": "内部规则：系统高不确定性复核机制说明",
            "docSourceCode": "DEMO-DOC-005",
            "sourceUri": "demo://doc/005",
            "docVersion": KNOWLEDGE_VERSION,
            "reviewStatusCode": "APPROVED",
            "orgId": ORG_ID,
            "contentText": "当模型输出不确定性概率（Uncertainty Score）> 0.35 时，由于处于可信度边缘地带，系统将强制附加 HIGH_UNCERTAINTY 原因，并将审查标记位置位（reviewSuggestedFlag=1），阻断自动归档流转。"
        }
    ]

    print("Documents ready for indexing:")
    for d in documents:
        print(f"  - [{d['docSourceCode']}] {d['contentText'][:40]}...")

    is_ready = wait_for_backend()
    if is_ready:
        try:
            for doc in documents:
                doc_url = f"{PYTHON_BACKEND_URL}/ai/v1/knowledge/documents"
                resp = requests.post(doc_url, json=doc, timeout=5)
                if resp.status_code not in [200, 201, 202]:
                    print(f"Failed to insert doc {doc['docSourceCode']}: {resp.text}")

            print("Triggering RAG Vector Index Rebuild...")
            rebuild_url = f"{PYTHON_BACKEND_URL}/ai/v1/knowledge/rebuild"
            rebuild_payload = {
                "kbCode": KB_CODE,
                "knowledgeVersion": KNOWLEDGE_VERSION,
                "orgId": ORG_ID
            }
            r_resp = requests.post(rebuild_url, json=rebuild_payload, timeout=15)
            if r_resp.status_code in [200, 201, 202]:
                print("\nKnowledge Vector Index rebuilt and seeded successfully via API.")
            else:
                print(f"\nAPI Rebuild returned {r_resp.status_code}: {r_resp.text}")
                
        except Exception as e:
            print(f"\nFailed to connect to API ({e}). Assuming offline demo mode.")
    else:
        print("\nBackend not reachable. Skipping real indexing. Seed complete for offline demo.")

if __name__ == "__main__":
    seed_knowledge()
