# Goal Detail Smoke Test

대상: `goal_list.html`, `goal_detail.html`  
기준일: 2026-03-05

## 체크리스트
1. 목표 목록 카드 탭 시 `selectedGoalTitle` 저장 후 `goal_detail`로 이동한다.
2. API 목표 카드 탭 시 `selectedGoalId`가 함께 저장된다.
3. ID 없는 카드(샘플/정적) 탭 시 `selectedGoalId`가 남지 않는다.
4. 목표 상세 진입 시 선택 목표의 카테고리/기간/달성률/진행단계가 화면에 반영된다.
5. 완료 처리 버튼 상태가 목표별 키(`goalDone:<id|title>`)로 독립 저장된다.

## 정적 점검(이번 턴)
1. `goal_list`에서 ID 없는 카드 선택 시 `selectedGoalId` 제거: PASS
2. `goal_detail` 데이터 하이드레이션(`TeamTodo.getGoals`) 존재: PASS
3. 목표 상세 UI 바인딩 ID(`goalCategory`, `goalPeriod`, `goalProgressPercent`, `goalProgressBar`) 추가: PASS
