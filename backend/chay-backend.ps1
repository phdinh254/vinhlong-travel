# Chạy backend mà không cần cài Maven: lần đầu tự tải Maven về thư mục backend\.maven
# Cách dùng (PowerShell, đứng trong thư mục backend):  .\chay-backend.cmd
$ErrorActionPreference = 'Stop'
$MavenVersion = '3.9.9'
$Root = $PSScriptRoot
$MavenHome = Join-Path $Root ".maven\apache-maven-$MavenVersion"
$MvnCmd = Join-Path $MavenHome 'bin\mvn.cmd'

# 1. Kiểm tra Java (Spring Boot 3 cần JDK 17 trở lên)
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Khong tim thay Java. Hay cai JDK 17 hoac 21 (vi du Eclipse Temurin tai https://adoptium.net) roi mo lai PowerShell." -ForegroundColor Red
    exit 1
}
# java -version in ra stderr: goi qua cmd de PowerShell khong coi la loi
$javaVer = (cmd /c "java -version 2>&1" | Select-Object -First 1)
$javaVer = "$javaVer"
Write-Host "Java: $javaVer"
if ($javaVer -match '"(1\.)?(\d+)') {
    if ([int]$Matches[2] -lt 17) {
        Write-Host "Can JDK 17 tro len, may dang dung Java $($Matches[2])." -ForegroundColor Red
        exit 1
    }
}

# 2. Tải Maven lần đầu
if (-not (Test-Path $MvnCmd)) {
    $zip = Join-Path $Root ".maven\maven.zip"
    New-Item -ItemType Directory -Force -Path (Join-Path $Root '.maven') | Out-Null
    $urls = @(
        "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip",
        "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip",
        "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
    )
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $ok = $false
    foreach ($url in $urls) {
        try {
            Write-Host "Dang tai Maven $MavenVersion tu $url ..."
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            $ok = $true
            break
        } catch {
            Write-Host "  That bai: $($_.Exception.Message)" -ForegroundColor Yellow
            if (Test-Path $zip) { Remove-Item $zip -Force }
        }
    }
    if (-not $ok) {
        Write-Host "Khong tai duoc Maven. Kiem tra mang/proxy/tuong lua, hoac tai thu cong (xem README muc 2.2)." -ForegroundColor Red
        exit 1
    }
    Expand-Archive -Path $zip -DestinationPath (Join-Path $Root '.maven') -Force
    Remove-Item $zip
}

# 3. Chạy backend (truyền thêm tham số nếu có, ví dụ: -Dspring-boot.run.profiles=dev-h2)
Set-Location $Root
if ($args.Count -gt 0) {
    & $MvnCmd spring-boot:run @args
} else {
    & $MvnCmd spring-boot:run
}
exit $LASTEXITCODE
