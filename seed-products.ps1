# seed-products.ps1
# Populates product-service with a realistic catalog so the frontend has something
# substantial to show. Run this instead of relying on the frontend's mock fallback -
# it exercises the real stack end to end (gateway -> product-service -> Postgres).
#
# Usage, from ecommerce\ with the stack running:
#   .\seed-products.ps1
#
# Safe to run more than once, though it will create duplicates - there is no
# uniqueness constraint on product name, deliberately (two suppliers can legitimately
# sell an item with the same name).

$gateway = "http://localhost:8090"

$products = @(
    @{ name="Aurora Wireless Mouse";       description="Ergonomic 6-button mouse with silent switches and USB-C charging."; price=29.99;  stockQuantity=142; category="Peripherals" },
    @{ name="Halcyon Mechanical Keyboard"; description="Hot-swappable 75% board with PBT keycaps and tactile brown switches."; price=129.00; stockQuantity=38;  category="Peripherals" },
    @{ name="Orbit 1440p Webcam";          description="Auto-framing webcam with dual noise-cancelling mics and privacy shutter."; price=119.00; stockQuantity=24; category="Peripherals" },
    @{ name="Relay Ergonomic Trackball";   description="Thumb-operated trackball designed to reduce wrist strain."; price=74.50;  stockQuantity=41;  category="Peripherals" },
    @{ name="Vantage 27-inch 4K Monitor";  description="IPS panel, 144Hz, 95% DCI-P3 coverage with a single-cable USB-C dock."; price=549.00; stockQuantity=12; category="Displays" },
    @{ name="Vantage 34-inch Ultrawide";   description="Curved 3440x1440 display with KVM switch and 120Hz refresh."; price=749.00; stockQuantity=5;   category="Displays" },
    @{ name="Echo Studio Headphones";      description="Over-ear ANC headphones with 40-hour battery and multipoint pairing."; price=249.99; stockQuantity=56; category="Audio" },
    @{ name="Pulse Compact Earbuds";       description="IPX5 earbuds with adaptive transparency and wireless charging case."; price=89.50;  stockQuantity=203; category="Audio" },
    @{ name="Cascade Streaming Mic";       description="Cardioid USB condenser mic with onboard gain and zero-latency monitoring."; price=139.00; stockQuantity=19; category="Audio" },
    @{ name="Nimbus 2TB NVMe SSD";         description="PCIe 4.0 drive rated 7,000MB/s read with a 5-year warranty."; price=179.00; stockQuantity=74;  category="Storage" },
    @{ name="Nimbus Portable SSD 1TB";     description="Pocket-sized USB-C drive, rugged aluminium shell, 1,050MB/s."; price=109.99; stockQuantity=63;  category="Storage" },
    @{ name="Meridian USB-C Hub";          description="9-in-1 hub with 100W passthrough, dual HDMI, and gigabit ethernet."; price=69.95;  stockQuantity=88;  category="Accessories" },
    @{ name="Atlas Laptop Stand";          description="Aluminium stand with six height positions and cable routing channel."; price=45.00;  stockQuantity=7;   category="Accessories" },
    @{ name="Lumen Desk Lamp";             description="Adjustable colour temperature with ambient light sensor and USB port."; price=79.00;  stockQuantity=31;  category="Accessories" },
    @{ name="Forge GaN Charger 100W";      description="Four-port GaN charger that fits a laptop and three devices at once."; price=59.99;  stockQuantity=167; category="Accessories" }
)

# Describes a failure usefully whether or not the server answered: ErrorDetails.Message holds
# the response body (only set when there IS a response), otherwise fall back to the exception.
function Get-FailureText($errorRecord) {
    $status = $null
    if ($errorRecord.Exception.Response) {
        $status = [int]$errorRecord.Exception.Response.StatusCode
    }
    $detail = $errorRecord.ErrorDetails.Message
    if (-not $detail) { $detail = $errorRecord.Exception.Message }
    if ($status) { return "HTTP $status - $detail" }
    return $detail
}

# Fail fast with one clear message rather than 15 identical ones if the stack is down.
try {
    Invoke-RestMethod -Uri "$gateway/actuator/health" -TimeoutSec 5 | Out-Null
} catch {
    Write-Host "Cannot reach the gateway at $gateway - is the stack running?" -ForegroundColor Red
    Write-Host "  $(Get-FailureText $_)" -ForegroundColor Red
    Write-Host "Start it with:  docker compose up -d --build" -ForegroundColor Yellow
    exit 1
}

# POST /api/products is NOT public at the gateway - only GET is (see JwtAuthGatewayFilter).
# So seeding needs a real token, which means registering a seed user and logging in.
$seedUser = @{ email="seed@example.com"; password="seed-password-123"; firstName="Seed"; lastName="Script" }

try {
    Invoke-RestMethod -Uri "$gateway/api/users/register" -Method Post `
                      -Body ($seedUser | ConvertTo-Json) -ContentType "application/json" | Out-Null
    Write-Host "Registered seed user $($seedUser.email)." -ForegroundColor DarkGray
} catch {
    # Already registered from an earlier run is fine - we only need to log in.
    Write-Host "Seed user already exists, logging in." -ForegroundColor DarkGray
}

try {
    $login = Invoke-RestMethod -Uri "$gateway/api/users/login" -Method Post `
                               -Body (@{ email=$seedUser.email; password=$seedUser.password } | ConvertTo-Json) `
                               -ContentType "application/json"
} catch {
    Write-Host "Could not log in as the seed user - $(Get-FailureText $_)" -ForegroundColor Red
    exit 1
}

$headers = @{ Authorization = "Bearer $($login.accessToken)" }

Write-Host "Seeding $($products.Count) products via $gateway ..." -ForegroundColor Cyan

$created = 0
foreach ($p in $products) {
    try {
        $body = $p | ConvertTo-Json
        $result = Invoke-RestMethod -Uri "$gateway/api/products" -Method Post -Headers $headers `
                                     -Body $body -ContentType "application/json"
        Write-Host ("  [{0,3}] {1}" -f $result.id, $result.name) -ForegroundColor Green
        $created++
    } catch {
        Write-Host ("  FAILED: {0} - {1}" -f $p.name, (Get-FailureText $_)) -ForegroundColor Red
    }
}

Write-Host "`nDone. $created of $($products.Count) products created." -ForegroundColor Cyan
Write-Host "Open http://localhost:3000/login.html to use the storefront."