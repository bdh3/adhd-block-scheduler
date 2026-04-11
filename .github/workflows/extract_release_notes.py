import re
import sys

def extract_latest_release_notes(readme_path):
    with open(readme_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # "## ✨ 주요 기능 (vX.X.X)" 섹션 찾기
    pattern = r"## ✨ 주요 기능 \(v\d+\.\d+\.\d+\)(.*?)## 🚀 정보"
    match = re.search(pattern, content, re.DOTALL)
    
    if match:
        notes = match.group(1).strip()
        # 불필요한 공백 제거 및 서식 정리
        return notes
    return "이번 릴리즈의 주요 변경 사항은 README.md를 참조해 주세요."

if __name__ == "__main__":
    print(extract_latest_release_notes('README.md'))
