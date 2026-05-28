@echo off
echo --------------------------------------------------
echo 🛡️ DeepShield File Organizer Starting...
echo --------------------------------------------------

:: 1. Sub-folders create karna (agar pehle se nahi bane hain)
echo Creating folder structure...
if not exist "backend-java\src\main\java\com\detector\auth" mkdir "backend-java\src\main\java\com\detector\auth"
if not exist "backend-java\src\main\java\com\detector\config" mkdir "backend-java\src\main\java\com\detector\config"
if not exist "backend-java\src\main\java\com\detector\controller" mkdir "backend-java\src\main\java\com\detector\controller"
if not exist "backend-java\src\main\java\com\detector\dto" mkdir "backend-java\src\main\java\com\detector\dto"
if not exist "backend-java\src\main\java\com\detector\entity" mkdir "backend-java\src\main\java\com\detector\entity"
if not exist "backend-java\src\main\java\com\detector\repository" mkdir "backend-java\src\main\java\com\detector\repository"
if not exist "backend-java\src\main\java\com\detector\service" mkdir "backend-java\src\main\java\com\detector\service"
if not exist "backend-java\src\main\resources\static" mkdir "backend-java\src\main\resources\static"
if not exist "ml-service-python\app\detection" mkdir "ml-service-python\app\detection"

:: 2. Root files ko unki jagah bhejna
if exist "docker-compose.yml" echo Root files structured.
if exist "Dockerfile" (
    echo [ALERT] Root me Dockerfile mili. Java ya Python? 
    echo Defaulting to current root status, please separate them manually if needed.
)

:: 3. Java Files Code Routing
echo Organizing Java Backend Files...
move /Y AuthController.java "backend-java\src\main\java\com\detector\auth\" 2>nul
move /Y JwtUtil.java "backend-java\src\main\java\com\detector\auth\" 2>nul
move /Y JwtAuthFilter.java "backend-java\src\main\java\com\detector\auth\" 2>nul
move /Y SecurityConfig.java "backend-java\src\main\java\com\detector\auth\" 2>nul

move /Y AsyncConfig.java "backend-java\src\main\java\com\detector\config\" 2>nul
move /Y WebSocketConfig.java "backend-java\src\main\java\com\detector\config\" 2>nul

move /Y DetectionController.java "backend-java\src\main\java\com\detector\controller\" 2>nul
move /Y ReportController.java "backend-java\src\main\java\com\detector\controller\" 2>nul

move /Y AuthDto.java "backend-java\src\main\java\com\detector\dto\" 2>nul
move /Y DetectionResultDto.java "backend-java\src\main\java\com\detector\dto\" 2>nul
move /Y TaskStatusDto.java "backend-java\src\main\java\com\detector\dto\" 2>nul

move /Y DetectionHistory.java "backend-java\src\main\java\com\detector\entity\" 2>nul
move /Y User.java "backend-java\src\main\java\com\detector\entity\" 2>nul

move /Y DetectionHistoryRepository.java "backend-java\src\main\java\com\detector\repository\" 2>nul
move /Y UserRepository.java "backend-java\src\main\java\com\detector\repository\" 2>nul

move /Y AsyncDetectionService.java "backend-java\src\main\java\com\detector\service\" 2>nul
move /Y DetectionHistoryService.java "backend-java\src\main\java\com\detector\service\" 2>nul
move /Y ReportGenerationService.java "backend-java\src\main\java\com\detector\service\" 2>nul
move /Y DetectorApplication.java "backend-java\src\main\java\com\detector\" 2>nul

move /Y application.properties "backend-java\src\main\resources\" 2>nul
move /Y index.html "backend-java\src\main\resources\static\" 2>nul
move /Y style.css "backend-java\src\main\resources\static\" 2>nul
move /Y app.js "backend-java\src\main\resources\static\" 2>nul
move /Y pom.xml "backend-java\" 2>nul

:: 4. Python ML Files Code Routing
echo Organizing Python ML Service Files...
move /Y main.py "ml-service-python\app\" 2>nul
move /Y requirements.txt "ml-service-python\" 2>nul
move /Y image_detector.py "ml-service-python\app\detection\" 2>nul
move /Y audio_detector.py "ml-service-python\app\detection\" 2>nul
move /Y video_detector.py "ml-service-python\app\detection\" 2>nul

echo --------------------------------------------------
echo ✅ All files moved to their respective folders!
echo --------------------------------------------------
pause