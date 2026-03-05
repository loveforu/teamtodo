# Friend Todo Smoke Test

대상 화면: `app/src/main/assets/html/friend_todo.html`  
기준일: 2026-03-05

## 사전 조건
1. `profile_select`에서 친구 프로필을 선택해 `targetProfileId`가 설정된 상태.
2. 대상 친구 프로필에 오늘 날짜 TODO가 최소 2개 이상 존재.
3. TODO 중 완료/미완료가 섞여 있고, 가능하면 서브 TODO(부모-자식) 1세트 포함.

## 스모크 체크리스트
1. 화면 최초 진입 시 헤더 포인트가 완료 TODO 포인트 합계와 일치한다.
2. 완료 TODO는 제목/서브텍스트가 취소선으로 표시되고, 미완료 TODO는 일반 표시된다.
3. 각 TODO 카드에 `+Xp` 뱃지가 보이며 값이 저장된 포인트와 일치한다.
4. 응원(하트) 버튼 탭 시:
5. 하트 아이콘이 채워지고, 응원 수가 1 증가한다.
6. 응원 취소 시 하트 아이콘이 비워지고, 응원 수가 1 감소한다.
7. 응원 토글 시 헤더 포인트 값은 변하지 않는다.
8. 코멘트 입력 후 전송 시 응원 목록(thumbs up)에 신규 항목이 반영된다.
9. 응원한 친구 목록 모달에서 `giverUid/comment`가 정상 노출된다.
10. 하단 네비 5개 메뉴(`todo_main`, `calendar`, `team_list`, `goal_list`, `settings`) 이동이 정상 동작한다.
11. 뒤로가기 버튼 탭 시 `profile_select`로 이동한다.

## 이번 턴 정적 점검 결과
1. 하드코딩 포인트 문구(`140/200`, `목표까지 60p`, `+2p`) 제거: PASS
2. 완료 TODO 기반 포인트 집계 함수(`computeCompletedPoints`) 존재: PASS
3. 초기 좋아요 동기화(`loadThumbsForTasks`) 존재: PASS
4. 응원/코멘트 후 단건 재동기화(`refreshSingleTaskThumbs`) 존재: PASS

## 참고
1. 현재 환경에서는 `gradlew.bat` 부재 + `bash` 실행 권한 제한으로 APK 빌드/실기기 검증은 미실행.
