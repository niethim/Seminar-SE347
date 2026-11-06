#!/usr/bin/env python3
import urllib.request
import json
import time
from concurrent.futures import ThreadPoolExecutor

BASE_URL = "http://localhost:8080/be/api/vouchers/FLASH50"

def init_stock(stock=10):
    req = urllib.request.Request(f"{BASE_URL}/init?stock={stock}", method="POST")
    with urllib.request.urlopen(req) as res:
        data = json.loads(res.read().decode())
        print(f"[*] Reset kho vé ban đầu: Total = {data['totalStock']}, Available = {data['availableStock']}\n")

def claim_voucher(user_idx):
    payload = json.dumps({"userId": f"competitor_user_{user_idx}", "simulateFraud": False}).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE_URL}/claim",
        data=payload,
        headers={"Content-Type": "application/json", "X-Trace-Id": f"trace-race-{user_idx}"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            return (res.status, res.read().decode())
    except urllib.error.HTTPError as e:
        return (e.code, e.read().decode())
    except Exception as e:
        return (500, str(e))

def main():
    print("=" * 60)
    print("      MÀN 1: BẮN TRANH CHẤP ĐỒNG THỜI (RACE CONDITION TEST)")
    print("=" * 60)
    
    TOTAL_REQUESTS = 50
    CONCURRENT_THREADS = 15
    INITIAL_STOCK = 10
    
    init_stock(INITIAL_STOCK)
    
    print(f"[*] Đang bắn đồng thời {TOTAL_REQUESTS} requests với {CONCURRENT_THREADS} threads song song...")
    start_time = time.time()
    
    with ThreadPoolExecutor(max_workers=CONCURRENT_THREADS) as executor:
        results = list(executor.map(claim_voucher, range(1, TOTAL_REQUESTS + 1)))
        
    duration = time.time() - start_time
    print(f"[*] Hoàn tất trong: {duration:.2f}s (~{TOTAL_REQUESTS / duration:.1f} req/s)\n")
    
    status_counts = {}
    for code, body in results:
        status_counts[code] = status_counts.get(code, 0) + 1
        
    print("=== THỐNG KÊ KẾT QUẢ HTTP STATUS CODE ===")
    for code, count in sorted(status_counts.items()):
        label = "202 ACCEPTED (Claim thành công)" if code == 202 else f"{code} (Bị từ chối / Hết kho)"
        print(f"  HTTP {code}: {count} requests -> {label}")
        
    req = urllib.request.Request(BASE_URL, method="GET")
    with urllib.request.urlopen(req) as res:
        stock = json.loads(res.read().decode())
        
    print("\n=== KIỂM TRA TRẠNG THÁI KHO CUỐI CÙNG ===")
    print(f"  Tổng kho ban đầu : {stock['totalStock']}")
    print(f"  Tồn kho khả dụng : {stock['availableStock']}")
    
    if stock['availableStock'] == 0 and status_counts.get(202, 0) == INITIAL_STOCK:
        print("\n=> [KẾT LUẬN]: CHỨNG MINH THÀNH CÔNG!")
        print(f"   Đúng chính xác {INITIAL_STOCK} vé thành công.")
        print(f"   Đúng chính xác {TOTAL_REQUESTS - INITIAL_STOCK} vé bị từ chối.")
        print("   Tồn kho về đúng 0. Không overselling, không double-booking!")
    else:
        print("\n=> [CẢNH BÁO]: Kết quả không khớp!")

if __name__ == "__main__":
    main()
