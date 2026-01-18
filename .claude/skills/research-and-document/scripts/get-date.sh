#!/bin/bash
# 取得當前日期資訊

echo "=== 當前時間資訊 ==="
echo "今天日期: $(date +%Y-%m-%d)"
echo "完整時間: $(date +%Y-%m-%d\ %H:%M:%S)"
echo "年份: $(date +%Y)"
echo ""
echo "=== 搜尋建議 ==="
echo "搜尋時請使用年份: $(date +%Y)"
echo "如需較新資料，可加上: $(date +%Y) latest"
