rootProject.name = "ecommerce-platform"

include("libs:contracts", "libs:common-security", "libs:messaging")
include("services:auth-service", "services:product-service", "services:order-service", "services:payment-service")
