# Wait for Kibana to be ready
Write-Host "Waiting for Kibana to be ready on http://localhost:5601..."
$maxRetries = 30
$retryCount = 0
$kibanaReady = $false

while (-not $kibanaReady -and $retryCount -lt $maxRetries) {
    try {
        $res = Invoke-WebRequest -Uri "http://localhost:5601/api/status" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($res.StatusCode -eq 200) {
            $kibanaReady = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 3
        $retryCount++
        Write-Host "Kibana starting up ($retryCount/$maxRetries)..."
    }
}

if (-not $kibanaReady) {
    Write-Warning "Kibana did not respond in time. Please try running this script again once Kibana is UP."
    exit 1
}

Write-Host "Kibana is UP! Creating Data View 'ecommerce-logs-*'..."

$body = @{
    data_view = @{
        title = "ecommerce-*"
        name = "Ecommerce Microservices Logs (All)"
        timeFieldName = "@timestamp"
    }
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:5601/api/data_views/data_view" `
        -Method Post `
        -Body $body `
        -ContentType "application/json" `
        -Headers @{ "kbn-xsrf" = "true" }
    Write-Host "Successfully created Data View: $($response.data_view.name) (ID: $($response.data_view.id))" -ForegroundColor Green
} catch {
    if ($_.Exception.Message -match "Conflict" -or $_.Exception.Response.StatusCode -eq 409) {
        Write-Host "Data View 'ecommerce-logs-*' already exists." -ForegroundColor Yellow
    } else {
        Write-Host "Info: $($_.Exception.Message)"
    }
}

Write-Host "Done! You can open http://localhost:5601/app/discover to explore logs." -ForegroundColor Cyan
