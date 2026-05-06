# Mobile & Web App Development Challenges

## App List

- [ ] 1. [Baby Grow App](./01-baby-grow-app.md)
- [ ] 2. [Learning Russian App](./02-learning-russian-app.md)
- [ ] 3. [Notion App](./03-notion-app.md)
- [ ] 4. [Tour App](./04-tour-app.md)
- [ ] 5. [Hotel Service Management App](./05-hotel-service-management-app.md)
- [ ] 6. [Agriculture Mobile App](./06-agriculture-mobile-app.md)
- [ ] 7. [Clinic Management App](./07-clinic-management-app.md)
- [ ] 8. [IoT Smart Home](./08-iot-smart-home.md)
- [ ] 9. [Girl's Health Management](./09-girls-health-management.md)
- [ ] 10. [Caroli Analyze (Calorie Tracker)](./10-caroli-analyze.md)
- [ ] 11. [Shop Assets Management App (RFID)](./11-shop-assets-management-rfid.md)
- [ ] 12. [Yoga App](./12-yoga-app.md)
- [ ] 13. [Health-Track (Detect System) - Using AI Model](./13-health-track-ai.md)
- [ ] 14. [AI Model Setup for HealthCare & Agriculture](./14-ai-model-healthcare-agriculture.md)
- [ ] 15. [Height Increase App](./15-height-increase-app.md)
- [ ] 16. [Women Health Management App](./16-women-health-management-app.md)
- [ ] 17. [AR/AI Hair Style App](./17-ar-ai-hair-style-app.md)


3. 🍲 종합 영양+이미지 데이터셋 사용하기 (로컬 데이터베이스 구축)
API 호출이 아니라, 미리 구축된 종합 데이터베이스를 통째로 다운로드하여 사용하는 방법입니다.

종합 식품 데이터베이스(Comprehensive Food Database): GitHub에 공개된 lxaw/ComprehensiveFoodDatabase 프로젝트가 대표적입니다. 이는 USDA의 원천 데이터를 가공하여 음식명과 함께 수십만 개의 이미지 파일까지 포함한 거대한 MySQL 데이터베이스입니다.

적용 방법: 이 리포지토리에서 데이터 덤프 파일을 다운로드받아 로컬 서버에 구축하여 사용할 수 있습니다.

장점: 외부 API 의존도가 낮아지고 빠른 응답 속도를 기대할 수 있습니다.

단점: 데이터 용량이 매우 크고(이미지 파일만 수십만 개), 로컬 서버 관리가 필요합니다.

4. 🔄 대체 API 사용하기 (가장 간편한 방법)
처음부터 이미지를 포함한 데이터를 제공하는 다른 영양/식품 API로 완전히 갈아타는 것도 좋은 방법입니다. 대부분의 서비스는 개발자를 위한 무료 요금제를 제공합니다.

API 서비스	주요 특징	가격 정책
FatSecret Platform API	음식 이미지, 알레르기 정보, 방대한 레시피 데이터베이스 등 고급 기능 지원	무료 요금제 (출처 표기 의무) 및 유료 요금제 제공
Edamam API	자연어 검색, 식이 제한 필터링 등 강력한 검색 기능	무료 요금제 (월 1,000회 호출) 및 유료 요금제 제공
Open Food Facts API	커뮤니티 기반의 방대한 데이터베이스, 바코드 기반 이미지 제공	완전 무료, RESTful API 제공
Nutritionix API	방대한 식품 데이터베이스, 자연어 검색 지원	무료 요금제 및 유료 요금제 제공