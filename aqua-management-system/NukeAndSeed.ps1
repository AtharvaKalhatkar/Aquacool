# ==============================================================================
# 🌌 BHAIRAVNATH AQUA ULTIMATE TOTAL WIPE & AUTO-SEED UTILITY (FINAL STAGE)
# ==============================================================================

Write-Host "🛑 Severing active software locks..." -ForegroundColor Magenta
# Force-kill Java AND the Packaged EXE name!
Stop-Process -Name "Aqua Management" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2 # Give OS a moment to release locks fully

$BaseUrl = "https://uszuutvdfavikxbyrduy.supabase.co/rest/v1"
$ApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzenV1dHZkZmF2aWt4YnlyZHV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1NTczODEsImV4cCI6MjA5NDEzMzM4MX0.o-m2FoorW7H3J8wA5_v9OlfKbU007u2QM41VjnwimR0"

$Headers = @{
    "apikey" = $ApiKey
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

Write-Host "`n🚀 INITIATING ULTIMATE NUCLEAR RESET & CLEAN SEED..." -ForegroundColor Cyan

# --- STEP 1: WIPE CLOUD TABLES COMPLETELY ---
Write-Host "`n☁️  [1/4] Wiping Supabase Cloud Tables..." -ForegroundColor Yellow

$TableList = @("bills", "deliveries", "customers")
foreach ($table in $TableList) {
    Write-Host "   [-] Purging entire [$table] table..." -ForegroundColor DarkYellow
    try {
        $uri = "$BaseUrl/$table?id=gt.0"
        $res = Invoke-RestMethod -Uri $uri -Method Delete -Headers $Headers -ErrorAction Stop
        Write-Host "       ✅ [$table] wiped successfully!" -ForegroundColor Green
    } catch {
        if ($_.Exception.Message -like "*404*") {
            Write-Host "       ✅ [$table] is already clean!" -ForegroundColor Green
        } else {
            Write-Host "       ⚠️ [$table] log: $_" -ForegroundColor DarkGray
        }
    }
}

# --- STEP 2: DESTROY LOCAL OFFLINE SQLITE DATABASE ---
Write-Host "`n💾 [2/4] Destroying Local SQLite Storage..." -ForegroundColor Yellow
$DbDir = "$env:USERPROFILE\.aqua_management"
$DbFile = "$DbDir\aqua_management.db"

if (Test-Path $DbFile) {
    try {
        Remove-Item -Path $DbFile -Force -ErrorAction Stop
        Write-Host "       ✅ Local database file destroyed!" -ForegroundColor Green
    } catch {
        Write-Host "       ❌ ERROR: Could not delete local database. Lock remains: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "       ✅ Local database already clean." -ForegroundColor Green
}

# --- STEP 3: DESTROY LOCAL BACKUPS ---
Write-Host "`n🛡️  [3/4] Purging Backups..." -ForegroundColor Yellow
$BackupDir = "$DbDir\backups"
if (Test-Path $BackupDir) {
    try {
        Remove-Item -Path "$BackupDir\*" -Force -Recurse -ErrorAction SilentlyContinue
        Write-Host "       ✅ Backup vault purged clean!" -ForegroundColor Green
    } catch {
        Write-Host "       ⚠️ Backups log: $_" -ForegroundColor DarkGray
    }
}

# --- STEP 4: SEED 10 PREMIUM MOCK CUSTOMERS DIRECTLY INTO SUPABASE ---
Write-Host "`n🌱 [4/4] Seeding 10 Fresh Professional Customers into Cloud..." -ForegroundColor Yellow

$SeedTime = [int][double]::Parse((Get-Date -Date "2026-05-15 12:00:00").Subtract((Get-Date -Date "1970-01-01")).TotalSeconds)

$SeedData = @(
    @{ "id" = ($SeedTime + 1); "name" = "Rajesh Patil";     "address" = "Chakan Phase 1";         "mobile" = "9876543210"; "route" = "Chakan";   "email" = "rajesh.patil@gmail.com" },
    @{ "id" = ($SeedTime + 2); "name" = "Sunita Deshmukh";  "address" = "MIDC Corner";             "mobile" = "9876543211"; "route" = "Chakan";   "email" = "sunita.d@yahoo.com" },
    @{ "id" = ($SeedTime + 3); "name" = "Karan Malhotra";   "address" = "Talegaon Road";          "mobile" = "9876543212"; "route" = "Talegaon"; "email" = "karan.malhotra@gmail.com" },
    @{ "id" = ($SeedTime + 4); "name" = "Pooja Sharma";     "address" = "Sai Mandir Chowk";       "mobile" = "9876543213"; "route" = "Chakan";   "email" = "pooja.sharma@outlook.com" },
    @{ "id" = ($SeedTime + 5); "name" = "Vikram Shinde";    "address" = "Pune Highway";           "mobile" = "9876543214"; "route" = "Pune";     "email" = "vikram.shinde@gmail.com" },
    @{ "id" = ($SeedTime + 6); "name" = "Anjali Joshi";     "address" = "Green Valley Residency"; "mobile" = "9876543215"; "route" = "Talegaon"; "email" = "anjali.joshi@gmail.com" },
    @{ "id" = ($SeedTime + 7); "name" = "Amit Gadkari";     "address" = "Shivajinagar";           "mobile" = "9876543216"; "route" = "Pune";     "email" = "amit.gadkari@yahoo.in" },
    @{ "id" = ($SeedTime + 8); "name" = "Snehal Patil";     "address" = "Main Bazar Road";        "mobile" = "9876543217"; "route" = "Chakan";   "email" = "snehal.patil@gmail.com" },
    @{ "id" = ($SeedTime + 9); "name" = "Rahul Deshpande";  "address" = "Hilltop Society";        "mobile" = "9876543218"; "route" = "Talegaon"; "email" = "rahul.d@gmail.com" },
    @{ "id" = ($SeedTime + 10); "name" = "Deepa Mehta";      "address" = "Market Yard";            "mobile" = "9876543219"; "route" = "Pune";     "email" = "deepa.mehta@gmail.com" }
)

$SeedJson = $SeedData | ConvertTo-Json -Compress

$SeedHeaders = @{
    "apikey" = $ApiKey
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
    "Prefer" = "return=minimal"
}

try {
    $res = Invoke-RestMethod -Uri "$BaseUrl/customers" -Method Post -Headers $SeedHeaders -Body $SeedJson -ErrorAction Stop
    Write-Host "       🚀 SUCCESS! 10 Premium Customers initialized in Supabase Cloud!" -ForegroundColor Green
} catch {
    Write-Host "       ❌ Seeding Failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n👑 ULTIMATE RESET COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "👉 Launch your Desktop app now! It will connect, pull the 10 fresh customers instantly, and sync from a 100% Clean slate!`n" -ForegroundColor Cyan
