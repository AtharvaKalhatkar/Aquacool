# ==============================================================================
# 🧼 BHAIRAVNATH AQUA SYSTEM PURGE UTILITY (V3.1 MASTER SLATE)
# ==============================================================================

$BaseUrl = "https://uszuutvdfavikxbyrduy.supabase.co/rest/v1"
$ApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzenV1dHZkZmF2aWt4YnlyZHV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1NTczODEsImV4cCI6MjA5NDEzMzM4MX0.o-m2FoorW7H3J8wA5_v9OlfKbU007u2QM41VjnwimR0"

$Headers = @{
    "apikey" = $ApiKey
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

Write-Host "`n🚀 INITIALIZING SYSTEM RESET ENGINE..." -ForegroundColor Cyan

# --- STEP 1: WIPE SUPABASE CLOUD TABLES ---
Write-Host "`n☁️  Step 1: Wiping Cloud Supabase Data Stores..." -ForegroundColor Yellow

Write-Host "   [-] Purging Bills registry..." -ForegroundColor DarkYellow
try {
    $res1 = Invoke-RestMethod -Uri "$BaseUrl/bills?id=gt.0" -Method Delete -Headers $Headers -ErrorAction Stop
    Write-Host "       ✅ Bills Purged Successfully!" -ForegroundColor Green
} catch {
    Write-Host "       ⚠️ Bills Purge Log: $_" -ForegroundColor DarkGray
}

Write-Host "   [-] Purging Deliveries log..." -ForegroundColor DarkYellow
try {
    $res2 = Invoke-RestMethod -Uri "$BaseUrl/deliveries?id=gt.0" -Method Delete -Headers $Headers -ErrorAction Stop
    Write-Host "       ✅ Deliveries Purged Successfully!" -ForegroundColor Green
} catch {
    Write-Host "       ⚠️ Deliveries Purge Log: $_" -ForegroundColor DarkGray
}

Write-Host "   [-] Purging Customers index..." -ForegroundColor DarkYellow
try {
    $res3 = Invoke-RestMethod -Uri "$BaseUrl/customers?id=gt.0" -Method Delete -Headers $Headers -ErrorAction Stop
    Write-Host "       ✅ Customers Purged Successfully!" -ForegroundColor Green
} catch {
    Write-Host "       ⚠️ Customers Purge Log: $_" -ForegroundColor DarkGray
}


# --- STEP 2: WIPE LOCAL SQLITE DATABASE ---
Write-Host "`n💾 Step 2: Destroying Local Offline SQLite Stores..." -ForegroundColor Yellow
$DbDir = "$env:USERPROFILE\.aqua_management"
$DbFile = "$DbDir\aqua_management.db"

if (Test-Path $DbFile) {
    Write-Host "   [-] Found active database at: $DbFile" -ForegroundColor DarkYellow
    try {
        Remove-Item -Path $DbFile -Force -ErrorAction Stop
        Write-Host "       ✅ Primary SQLite Database file deleted!" -ForegroundColor Green
    } catch {
        Write-Host "       ❌ Error deleting database: $_" -ForegroundColor Red
        Write-Host "       💡 PLEASE CLOSE THE DESKTOP SOFTWARE FIRST and run this again!" -ForegroundColor Cyan
    }
} else {
    Write-Host "   ✅ No local database file detected to delete." -ForegroundColor Green
}

# --- STEP 3: WIPE LOCAL SQLITE BACKUPS ---
$BackupDir = "$DbDir\backups"
if (Test-Path $BackupDir) {
    Write-Host "   [-] Found active backups directory: $BackupDir" -ForegroundColor DarkYellow
    try {
        Remove-Item -Path "$BackupDir\*" -Force -Recurse -ErrorAction Stop
        Write-Host "       ✅ All historical database backups purged!" -ForegroundColor Green
    } catch {
        Write-Host "       ⚠️ Error clearing backups directory: $_" -ForegroundColor DarkGray
    }
}

Write-Host "`n🎉 SYSTEM PURGE COMPLETE! SYSTEM IS NOW 100% FRESH & READY!" -ForegroundColor Green
Write-Host "👉 Open your Desktop app now to start recording clean, duplicate-free logs!`n" -ForegroundColor Cyan
