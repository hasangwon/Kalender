---
name: git-push
description: main 브랜치 원격 푸시. 원격 미설정이면 개인 계정용 SSH 별칭으로 생성 안내. '/push'.
---

# Git Push Skill

> 변경사항을 origin에 푸시

---

## 개요

이 스킬은 커밋된 변경사항을 원격 저장소에 푸시합니다.
개인 프로젝트라 단일 `main` 브랜치를 사용합니다.

**호출 방법**: `/push` 또는 "푸시해줘"

---

## 핵심 규칙

- **푸시 전 사용자 확인 필수** — 커밋과 푸시는 별개 승인
- 이 컴퓨터는 회사 GitHub 계정과 공존 환경 — **개인 계정은 SSH 별칭 `github-hasangwon` 사용**
  - remote URL 형식: `git@github-hasangwon:hasangwon/{repo}.git`
  - `git@github.com:...` 형식으로 추가하면 회사 인증과 꼬임

---

## 워크플로우

### 원격이 없는 경우 (최초)

```
1. 사용자에게 GitHub에 repo 생성 요청 (private 권장)
   - 또는 gh CLI가 개인 계정이면: 사용 불가(회사 계정) → 웹에서 생성

2. git remote add origin git@github-hasangwon:hasangwon/plan-widget.git

3. git push -u origin main
```

### 원격이 있는 경우

```
1. git status 로 미커밋 변경 확인
   └─ 있으면 → /commit 먼저 제안

2. git push
   └─ 거부(rejected) → git pull --rebase 후 재시도 제안

3. 결과 보고 (푸시된 커밋 요약)
```

---

## 주의사항

1. `gh` CLI는 **회사 계정(pling-sangwon)으로 로그인**되어 있음 — 이 저장소 원격 생성/PR에 사용 금지
2. force push 금지 (사용자가 명시 요청 시에만)
3. 푸시 후 원격 로그 확인: `git log --oneline origin/main -3`
