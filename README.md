# 어썸 예약 비서 (Asome Booking Helper)

네이버 스마트플레이스(예약 관리자)의 알림을 감지하여, 고객 정보 추출, 주소록 자동 저장, 그리고 맞춤형 안내 문자 발송(자동/수동)까지 원스톱으로 처리해 주는 안드로이드 자동화 어시스턴트 앱입니다.


## 💡 주요 기능 (Key Features)

1. **스마트 알림 감지 및 1차 파싱 (`MyNotificationListener`)**
   - 네이버 스마트플레이스 앱의 예약 확정 알림을 실시간으로 수신합니다.
   - 알림 텍스트에서 지점(`어썸기타학원 여의도점` / `어썸보컬드럼 여의도점`)과 예약자 이름을 1차로 식별합니다.
2. **접근성 기반 상세 모달 앵커링 파싱 (`MyAccessibilityService`)**
   - 알림 클릭 시 실행되는 스마트플레이스 WebView의 DOM 구조를 분석합니다.
   - 뒷배경의 과거 예약 목록 리스트를 완벽히 무시하고, 화면에 팝업된 `"예약 상세정보"` 모달 구간만을 앵커링(Anchoring)하여 정확한 단일 예약 건의 데이터(전화번호, 상품, 이용일시, 요청사항)를 추출합니다.
3. **주소록 자동 저장**
   - 추출된 고객 이름, 과목, 지점을 사용자가 설정한 포맷에 맞춰 기기 주소록에 자동으로 등록합니다.
4. **당일 중복/변경 예외 처리 및 발송 모드 분기**
   - 당일에 동일 번호로 접수된 예약이 있는 경우 `DUPLICATE` 상태로 분리하여 중복 문자 발송을 방지합니다.
   - **자동 모드(`AUTO`)**: 조건 충족 시 배경에서 즉시 안내 문자를 전송합니다.
   - **수동 모드(`MANUAL`)**: 바텀시트 UI를 통해 내용을 최종 확인하고 원터치로 발송할 수 있습니다.
5. **발송 기록 관리 및 템플릿 설정**
   - 발송된 내역을 날짜별/상태별로 조회하고 검색할 수 있습니다.
   - 설정 화면에서 연락처 저장 형식과 문자 템플릿을 자유롭게 커스텀(`{이름} · {과목} · {지점}`)할 수 있습니다.



## 🛠️ 기술 스택 (Tech Stack)

* **Language**: Java
* **UI Components**: Fragment, RecyclerView, BottomSheetDialogFragment, Material Design 3
* **Local Database**: Room (SQLite)
* **Android Framework**: AccessibilityService, NotificationListenerService, Telephony (SmsManager), ContactsContract


## 📂 프로젝트 구조 (Project Architecture)

```text
com.example.bookinghelper
 ┣ data/
 ┃  ┣ AppDatabase.java      # Room Database 싱글톤 인스턴스
 ┃  ┣ BookingDao.java       # 예약 내역 데이터 접근 객체 (DAO)
 ┃  └ BookingItem.java      # 예약 정보 엔티티 모델
 ┣ service/
 ┃  ┣ MyAccessibilityService.java # 스마트플레이스 화면 UI 텍스트 파싱 및 액션 자동화
 ┃  └ MyNotificationListener.java # 네이버 예약 알림 수신 및 리다이렉트
 ┣ ui/
 ┃  ┣ HistoryFragment.java        # 발송 이력 및 상세 조회 뷰
 ┃  ┣ MainPagerAdapter.java       # 메인 탭 페이저 어댑터
 ┃  ┣ ReviewMessageBottomSheet.java # 수동 모드 발송 확인 바텀시트
 ┃  ┣ SettingsFragment.java       # 연락처/문자 템플릿 커스텀 설정 뷰
 ┃  └ ...
 └ util/
    ┣ BookingState.java     # 프로세스 전역 상태 및 타깃 변수 관리
    ┣ TemplateManager.java  # 템플릿 치환 및 저장소 관리 유틸
    └ TimeUtils.java        # 당일 시작/종료 타임스탬프 계산 유틸
```

## ⚙️ 동작 파이프라인 (Pipeline Workflow)

1. **알림 수신 (`[1]`)**: 네이버 예약 확정 푸시 알림 수신 $\rightarrow$ 지점 및 고객명 추출 $\rightarrow$ `BookingState.isProcessing = true`
2. **화면 진입 (`[2]`)**: 알림의 `contentIntent`를 자동 실행하여 스마트플레이스 상세 페이지로 이동
3. **텍스트 스캔 (`[3]`)**: `AccessibilityService`가 화면 진입을 감지하고 1.5초 후 대기 $\rightarrow$ 회차별 스캔 시작 (`최대 5회`)
4. **모달 앵커링 (`[4]`)**: 수집된 텍스트 리스트 중 `"예약 상세정보"` 키워드를 찾아 그 하위 구간만 파싱 (과거 예약 리스트 간섭 차단)
5. **DB 저장 및 후속 처리 (`[5]`)**: 
   - 주소록에 포맷된 이름으로 저장
   - 중복 여부 확인 후 `SENT`(자동) 또는 `PENDING`(수동 대기)으로 Room DB 적재
   - 스마트플레이스 화면을 닫고(`GLOBAL_ACTION_BACK`) 앱으로 복귀


## 🚀 설치 및 실행 방법 (Getting Started)

1. 안드로이드 스튜디오에서 프로젝트를 엽니다.
2. 갤럭시 등 실제 안드로이드 기기에 앱을 빌드 및 설치합니다.
3. 기기의 **설정 $\rightarrow$ 접근성**에서 **`어썸 예약 비서`** 접근성 서비스를 활성화합니다.
4. 기기의 **설정 $\rightarrow$ 알림 접근 허용**에서 **`어썸 예약 비서`**의 알림 리스너 권한을 허용합니다.
5. 앱 내 설정 탭에서 원하는 연락처 저장 형식과 문자 템플릿을 설정한 후 사용하세요!
