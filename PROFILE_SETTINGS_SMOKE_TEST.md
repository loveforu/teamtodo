# Profile/Settings Smoke Test

대상 화면: `profile_select.html`, `profile_manage.html`, `settings.html`  
기준일: 2026-03-05

## 스모크 체크리스트
1. `settings` 진입 직후 현재 프로필 카드가 `currentProfileId` 기준으로 즉시 노출된다.
2. `settings`에서 프로필 선택/관리 버튼으로 각각 `profile_select`/`profile_manage` 이동이 된다.
3. `profile_manage`에는 내 프로필만 표시되고 팀원 프로필은 노출되지 않는다.
4. `profile_manage`에서 수정 버튼 클릭 시 `avatar_select`로 진입하며 편집 데이터가 채워진다.
5. 프로필 목록 콜백 이벤트가 `onMyProfiles` 또는 `onProfilesForSelection`일 때 모두 화면이 갱신된다.
6. 프로필 데이터가 비어도 캐시/로컬 샘플 데이터가 있으면 목록이 대체 렌더링된다.
7. 다크 모드/테마 컬러를 바꾼 뒤 세 화면 재진입 시 동일 설정이 유지된다.

## 이번 턴 정적 점검 결과
1. `profile_manage` 캐시/로컬 렌더 시 `isMyAccount !== false` 필터 적용: PASS
2. `profile_manage` 이벤트 호환(`onMyProfiles`, `onProfilesForSelection`, `onProfiles`) 추가: PASS
3. `settings` 초기 렌더 시 캐시된 현재 프로필 복원(`getCachedCurrentProfile`) 추가: PASS
