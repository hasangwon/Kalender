# 업로드 키스토어

- `upload-keystore.jks` 와 비밀번호(`../keystore.properties`)는 **git 에 올라가지 않는다** (`.gitignore`).
- Play 앱 서명을 켜면 이 키는 "업로드 키" 로만 쓰이고, 배포 키는 구글이 관리한다.
- 분실하면 Play Console 에서 업로드 키 재설정을 신청할 수 있지만 며칠 걸리므로 두 파일을 따로 백업해 둔다.
- 생성 명령 (참고): `keytool -genkeypair -keystore upload-keystore.jks -storetype PKCS12 -alias upload -keyalg RSA -keysize 2048 -validity 10000`
