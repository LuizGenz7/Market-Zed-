package com.example.data.dev

import com.example.data.model.CategoryDetails
import com.example.data.model.ContactPreferences
import com.example.data.model.Listing
import com.example.data.model.ListingImage
import com.example.data.model.ListingLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DevFixturesProviderFactory {
    fun create(): DevFixturesProvider = DebugDevFixturesProvider()
}

private class DebugDevFixturesProvider : DevFixturesProvider {
    override val isSupported: Boolean = true

    override fun buildFixtures(nowMillis: Long): List<Listing> {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        fun isoDaysAgo(hoursAgo: Long): String = isoFmt.format(Date(nowMillis - hoursAgo * 3_600_000L))
        fun futureIso(daysAhead: Long): String = isoFmt.format(Date(nowMillis + daysAhead * 86_400_000L))

        val imgPhone = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80"
        val imgLaptop = "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=800&q=80"
        val imgTv = "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&w=800&q=80"
        val imgGaming = "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?auto=format&fit=crop&w=800&q=80"
        val imgFurniture = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80"
        val imgFashion = "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=800&q=80"
        val imgAppliance = "https://images.unsplash.com/photo-1584568694244-14fbdf83bd30?auto=format&fit=crop&w=800&q=80"
        val imgCar = "https://images.unsplash.com/photo-1549317661-bd82c8e0db2f?auto=format&fit=crop&w=800&q=80"
        val imgTools = "https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=800&q=80"
        val imgSolar = "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=800&q=80"
        val imgHouse = "https://images.unsplash.com/photo-1568605114967-8130f3a36994?auto=format&fit=crop&w=800&q=80"
        val imgApartment = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80"
        val imgRoom = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80"
        val imgShopOffice = "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80"
        val imgPlumbing = "https://images.unsplash.com/photo-1581578731548-c6464f3c388d?auto=format&fit=crop&w=800&q=80"
        val imgMechanic = "https://images.unsplash.com/photo-1486262715619-67b85e0b08d3?auto=format&fit=crop&w=800&q=80"
        val imgDesign = "https://images.unsplash.com/photo-1542744173-8e7e53415bb0?auto=format&fit=crop&w=800&q=80"
        val imgJob = "https://images.unsplash.com/photo-1521737711867-e3b97375f902?auto=format&fit=crop&w=800&q=80"

        val locations = listOf(
            ListingLocation("Lusaka", "Lusaka", "Lusaka District", "Kabulonga"),
            ListingLocation("Lusaka", "Lusaka", "Lusaka District", "Woodlands"),
            ListingLocation("Lusaka", "Lusaka", "Lusaka District", "Chelstone"),
            ListingLocation("Lusaka", "Chilanga", "Chilanga District", "Lilayi"),
            ListingLocation("Copperbelt", "Kitwe", "Kitwe District", "Riverside"),
            ListingLocation("Copperbelt", "Ndola", "Ndola District", "Kansenshi"),
            ListingLocation("Copperbelt", "Chingola", "Chingola District", "Nchanga North"),
            ListingLocation("Southern", "Livingstone", "Livingstone District", "Dambwa"),
            ListingLocation("Southern", "Choma", "Choma District", "Town Centre"),
            ListingLocation("Central", "Kabwe", "Kabwe District", "Highridge"),
            ListingLocation("Eastern", "Chipata", "Chipata District", "Moth Area"),
            ListingLocation("North-Western", "Solwezi", "Solwezi District", "Mushitala"),
            ListingLocation("Northern", "Kasama", "Kasama District", "Location"),
            ListingLocation("Luapula", "Mansa", "Mansa District", "Senama"),
            ListingLocation("Western", "Mongu", "Mongu District", "Lewanika Area"),
            ListingLocation("Muchinga", "Mpika", "Mpika District", "Chibansa")
        )

        fun contact(index: Int): ContactPreferences {
            val baseNum = 971000000 + index * 137
            val phone = "+260$baseNum"
            return when (index % 4) {
                0 -> ContactPreferences(phone = phone, whatsapp = phone, email = "fixture$index@dev.marketzed.test")
                1 -> ContactPreferences(phone = phone, whatsapp = phone, email = null)
                2 -> ContactPreferences(phone = phone, whatsapp = null, email = "fixture$index@dev.marketzed.test")
                else -> ContactPreferences(phone = null, whatsapp = phone, email = "fixture$index@dev.marketzed.test")
            }
        }

        // 1. 35 Products (marketplace)
        val productSpecs = listOf(
            Triple("Phones and accessories", "Samsung Galaxy A54 5G 128GB", 7600.0) to imgPhone,
            Triple("Phones and accessories", "iPhone 13 128GB Midnight Clean", 11800.0) to imgPhone,
            Triple("Phones and accessories", "Redmi Note 13 Pro 256GB Dual SIM", 5400.0) to imgPhone,
            Triple("Phones and accessories", "Tecno Camon 30 256GB Sealed", 4950.0) to imgPhone,
            Triple("Phones and accessories", "Anker 20,000mAh Fast Powerbank", 850.0) to imgPhone,
            Triple("Computers and laptops", "Lenovo ThinkPad T480 Core i5 16GB", 6900.0) to imgLaptop,
            Triple("Computers and laptops", "HP EliteBook 840 G7 512GB SSD", 9200.0) to imgLaptop,
            Triple("Computers and laptops", "Dell Latitude 5420 11th Gen i7", 10500.0) to imgLaptop,
            Triple("Computers and laptops", "Apple MacBook Air M1 256GB", 16800.0) to imgLaptop,
            Triple("Computers and laptops", "Epson EcoTank L3250 Wi-Fi Printer", 4300.0) to imgLaptop,
            Triple("TVs and electronics", "Hisense 55-inch 4K Smart Android TV", 8900.0) to imgTv,
            Triple("TVs and electronics", "Samsung 43-inch Full HD Smart TV", 6200.0) to imgTv,
            Triple("TVs and electronics", "JBL Flip 6 Bluetooth Speaker Original", 2400.0) to imgTv,
            Triple("TVs and electronics", "5kVA Hybrid Solar Inverter + 24V MPPT", 11200.0) to imgSolar,
            Triple("TVs and electronics", "300Ah Lithium Solar Backup Battery", 14500.0) to imgSolar,
            Triple("Gaming", "Sony PlayStation 5 Slim 1TB + 2 Pads", 16500.0) to imgGaming,
            Triple("Gaming", "Xbox Series S 512GB Clean Condition", 7800.0) to imgGaming,
            Triple("Gaming", "Nintendo Switch OLED White Edition", 8600.0) to imgGaming,
            Triple("Furniture", "6-Seater Zambian Mukwa Dining Set", 13500.0) to imgFurniture,
            Triple("Furniture", "Modern L-Shape Velvet Corner Sofa", 11900.0) to imgFurniture,
            Triple("Furniture", "Executive Ergonomic Office Chair", 2650.0) to imgFurniture,
            Triple("Furniture", "Queen Size Upholstered Bed Frame", 6400.0) to imgFurniture,
            Triple("Clothing and fashion", "Men's Slim Fit Charcoal Suit Size 42", 1850.0) to imgFashion,
            Triple("Clothing and fashion", "Chitenge Handcrafted Tote & Heels Set", 750.0) to imgFashion,
            Triple("Clothing and fashion", "Original Nike Air Max Running Shoes 43", 1950.0) to imgFashion,
            Triple("Home appliances", "Defy 260L Metallic Double Door Fridge", 7900.0) to imgAppliance,
            Triple("Home appliances", "LG 8kg Front-Loader Washing Machine", 9400.0) to imgAppliance,
            Triple("Home appliances", "Hisense 20L Digital Microwave Oven", 1950.0) to imgAppliance,
            Triple("Home appliances", "4-Plate Gas + Electric Standing Cooker", 5600.0) to imgAppliance,
            Triple("Vehicles", "2014 Toyota Harrier 2.0L Petrol Automatic", 235000.0) to imgCar,
            Triple("Vehicles", "2012 Honda Fit Hybrid Clean Duty Paid", 98000.0) to imgCar,
            Triple("Vehicles", "2015 Toyota Hilux Double Cab 4x4", 420000.0) to imgCar,
            Triple("Vehicle spare parts", "Toyota 1KZ Turbocharger Complete Kit", 4800.0) to imgMechanic,
            Triple("Building materials", "DeWalt 20V Cordless Hammer Drill Kit", 3600.0) to imgTools,
            Triple("Other goods", "Heavy-Duty 2000L PVC Water Storage Tank", 3200.0) to imgSolar
        )

        val products = productSpecs.mapIndexed { idx, (spec, imgUrl) ->
            val (sub, title, price) = spec
            val loc = locations[idx % locations.size]
            val hasDrop = idx % 6 == 1
            val promoted = idx in setOf(0, 5, 10, 13, 18)
            Listing(
                id = "dev_prod_${idx + 1}",
                ownerId = "dev_fixture_owner_${(idx % 8) + 1}",
                sellerName = "Test Merchant ${(idx % 8) + 1} (Dev)",
                sellerVerified = false,
                isPromoted = promoted,
                campaignFormat = if (promoted) (if (idx == 0) "carousel_1_3_1" else "sponsored_deal") else null,
                expiresAt = if (promoted) futureIso(14) else null,
                categoryId = "marketplace",
                subcategory = sub,
                title = title,
                description = "$title available in ${loc.displayFull}. Well maintained, ready for physical inspection before payment. Local pickup or arranged courier.",
                price = price,
                originalPrice = if (hasDrop) price * 1.18 else null,
                priceMode = "fixed",
                location = loc,
                images = listOf(ListingImage(url = imgUrl, thumbUrl = imgUrl)),
                contactPreferences = contact(idx),
                categoryDetails = CategoryDetails(
                    condition = if (idx % 3 == 0) "New" else if (idx % 3 == 1) "Like New" else "Used - Good",
                    brand = title.split(" ").firstOrNull(),
                    model = title.split(" ").take(3).joinToString(" ")
                ),
                moderationStatus = "live_reviewed",
                availabilityStatus = "available",
                isPublic = true,
                createdAt = isoDaysAgo((idx * 3 + 1).toLong()),
                publishedAt = isoDaysAgo((idx * 3 + 1).toLong())
            )
        }

        // 2. 20 Rental properties (STRICTLY FOR RENT ONLY)
        val rentalSpecs = listOf(
            Triple("Houses", "3-Bedroom Standalone House for Rent", 9500.0) to (imgHouse to "3"),
            Triple("Houses", "4-Bedroom Modern Executive House to Rent", 18000.0) to (imgHouse to "4"),
            Triple("Houses", "2-Bedroom Semi-Detached Cottage for Rent", 4800.0) to (imgHouse to "2"),
            Triple("Houses", "3-Bedroom Yard House with Borehole for Rent", 7500.0) to (imgHouse to "3"),
            Triple("Apartments", "2-Bedroom Newly Built Flat for Rent", 6500.0) to (imgApartment to "2"),
            Triple("Apartments", "1-Bedroom Furnished Apartment for Rent", 5500.0) to (imgApartment to "1"),
            Triple("Apartments", "3-Bedroom Luxury Apartment for Rent", 14500.0) to (imgApartment to "3"),
            Triple("Apartments", "2-Bedroom Ground Floor Flat to Rent", 5800.0) to (imgApartment to "2"),
            Triple("Rooms", "Self-Contained Bedsitter Room for Rent", 2200.0) to (imgRoom to "1"),
            Triple("Rooms", "Single Student Boarding Room Near UNZA", 1500.0) to (imgRoom to "1"),
            Triple("Rooms", "Furnished En-Suite Room to Rent Monthly", 2900.0) to (imgRoom to "1"),
            Triple("Short stays", "2-Bedroom Serviced Short Stay Apartment", 950.0) to (imgApartment to "2"),
            Triple("Short stays", "Cozy Studio Guest Suite Daily Rental", 650.0) to (imgRoom to "1"),
            Triple("Shops", "Ground Floor Retail Shop Space to Rent", 6000.0) to (imgShopOffice to "0"),
            Triple("Shops", "Road-Facing Boutique Shop Unit for Rent", 4500.0) to (imgShopOffice to "0"),
            Triple("Offices", "Open-Plan Office Suite for Rent with Backup Power", 11000.0) to (imgShopOffice to "0"),
            Triple("Offices", "3-Room Executive Office Wing to Let", 8500.0) to (imgShopOffice to "0"),
            Triple("Commercial property", "Corner Pharmacy / Clinic Space to Rent", 12500.0) to (imgShopOffice to "0"),
            Triple("Warehouses", "350sqm Industrial Warehouse Bay for Rent", 22000.0) to (imgShopOffice to "0"),
            Triple("Houses", "2-Bedroom Clean Wall-Fenced Flat for Rent", 4200.0) to (imgHouse to "2")
        )

        val rentals = rentalSpecs.mapIndexed { idx, (spec, media) ->
            val (sub, title, price) = spec
            val (imgUrl, beds) = media
            val loc = locations[(idx + 2) % locations.size]
            val promoted = idx in setOf(0, 4)
            val daily = sub == "Short stays"
            Listing(
                id = "dev_prop_${idx + 1}",
                ownerId = "dev_fixture_owner_${(idx % 6) + 10}",
                sellerName = if (idx % 2 == 0) "Zed Rentals Agency (Dev)" else "Landlord Test ${(idx % 6) + 1} (Dev)",
                sellerAccountType = if (idx % 2 == 0) "business" else "individual",
                sellerVerified = false,
                isPromoted = promoted,
                campaignFormat = if (promoted) "homepage_banner" else null,
                expiresAt = if (promoted) futureIso(14) else null,
                categoryId = "property",
                subcategory = sub,
                title = title,
                description = "$title in ${loc.displayFull}. Clean borehole water, paved yard, electric fence, and secure parking. Strictly for rent only.",
                price = price,
                priceMode = if (daily) "daily" else "monthly",
                location = loc,
                images = listOf(ListingImage(url = imgUrl, thumbUrl = imgUrl)),
                contactPreferences = contact(idx + 35),
                categoryDetails = CategoryDetails(
                    bedrooms = beds.takeIf { it != "0" },
                    bathrooms = if (beds == "0") "1" else beds,
                    amenities = "Borehole water, Paved yard, Electric fence, Carport"
                ),
                moderationStatus = "live_reviewed",
                availabilityStatus = "available",
                isPublic = true,
                createdAt = isoDaysAgo((idx * 4 + 2).toLong()),
                publishedAt = isoDaysAgo((idx * 4 + 2).toLong())
            )
        }

        // 3. 20 Services
        val serviceSpecs = listOf(
            Triple("Plumbing", "24/7 Emergency Plumbing & Borehole Pump Repairs", 450.0) to imgPlumbing,
            Triple("Electrical", "Licensed ZESCO Wiring & Solar Inverter Installation", 950.0) to imgSolar,
            Triple("Carpentry", "Custom Kitchen Units, Wardrobes & Doors", 1800.0) to imgFurniture,
            Triple("Mechanics", "Mobile Computerized Car Diagnostics & Servicing", 500.0) to imgMechanic,
            Triple("Cleaning", "Deep Sofa, Carpet & Post-Construction Cleaning", 650.0) to imgApartment,
            Triple("Painting", "Interior & Exterior Residential House Painting", 1500.0) to imgHouse,
            Triple("Graphic design", "Company Logos, Flyers & Social Media Branding", 400.0) to imgDesign,
            Triple("Web development", "Business Websites & Mobile Money Integrations", 3500.0) to imgLaptop,
            Triple("Photography", "Weddings, Graduations & Corporate Event Photography", 2500.0) to imgDesign,
            Triple("Tutoring", "ECZ Grade 12 Mathematics & Science Home Tuition", 350.0) to imgLaptop,
            Triple("Transport", "4-Tonne Light Truck House Moving & Deliveries", 1200.0) to imgCar,
            Triple("Phone repair", "Same-Day iPhone & Samsung Screen Replacement", 650.0) to imgPhone,
            Triple("Catering", "Corporate Lunches, Kitchen Parties & Braai Catering", 2200.0) to imgAppliance,
            Triple("Plumbing", "Geyser Installation, Toilet & Blocked Drain Clearing", 550.0) to imgPlumbing,
            Triple("Electrical", "DSTV Dish Alignment, CCTV & Electric Fence Setup", 850.0) to imgTools,
            Triple("Mechanics", "Auto Air-Con Regas & Suspension Alignment", 700.0) to imgMechanic,
            Triple("Cleaning", "Office & Shop Routine Janitorial Contracts", 1900.0) to imgShopOffice,
            Triple("Tutoring", "IELTS, ACCA & ZICA Exam Coaching Sessions", 600.0) to imgDesign,
            Triple("Transport", "Lusaka-Kitwe Safe Same-Day Parcel Courier", 180.0) to imgCar,
            Triple("Other services", "Borehole Drilling & Water Tank Stand Fabrication", 9500.0) to imgTools
        )

        val services = serviceSpecs.mapIndexed { idx, (spec, imgUrl) ->
            val (sub, title, price) = spec
            val loc = locations[(idx + 4) % locations.size]
            Listing(
                id = "dev_svc_${idx + 1}",
                ownerId = "dev_fixture_owner_${(idx % 5) + 20}",
                sellerName = "Service Pro ${(idx % 5) + 1} (Dev)",
                sellerAccountType = "business",
                sellerVerified = false,
                isPromoted = idx == 0 || idx == 3,
                expiresAt = if (idx == 0 || idx == 3) futureIso(10) else null,
                categoryId = "services",
                subcategory = sub,
                title = title,
                description = "$title covering ${loc.town} and surrounding areas in ${loc.district}. Neat workmanship, upfront quote before work starts.",
                price = price,
                priceMode = "starting_from",
                location = loc,
                images = listOf(ListingImage(url = imgUrl, thumbUrl = imgUrl)),
                contactPreferences = contact(idx + 55),
                categoryDetails = CategoryDetails(
                    serviceArea = "${loc.town} & surrounding districts"
                ),
                moderationStatus = "live_reviewed",
                availabilityStatus = "available",
                isPublic = true,
                createdAt = isoDaysAgo((idx * 5 + 3).toLong()),
                publishedAt = isoDaysAgo((idx * 5 + 3).toLong())
            )
        }

        // 4. 15 Jobs
        val jobSpecs = listOf(
            Triple("Full-time", "Registered Accountant (ZICA) — Copperbelt", 14500.0) to "ZamAgri Supplies (Dev)",
            Triple("Full-time", "Retail Shop Manager — Electronics Showroom", 7500.0) to "Copperbelt Tech Mart (Dev)",
            Triple("Full-time", "Heavy Duty Class CE Truck Driver", 8200.0) to "Kafue Haulage Ltd (Dev)",
            Triple("Part-time", "Social Media Content & Reels Creator", 3200.0) to "Lusaka Fashion Hub (Dev)",
            Triple("Full-time", "Qualified Solar & Electrical Technician", 9000.0) to "SunGrid Zambia (Dev)",
            Triple("Internships", "Junior Software Developer Graduate Intern", 3500.0) to "TechGU Test Studio (Dev)",
            Triple("Full-time", "Registered Nurse — Private Outpatient Clinic", 11000.0) to "Mosi Care Clinic (Dev)",
            Triple("Temporary", "Inventory Stock-Take Enumerators (2 Weeks)", 2800.0) to "Retail Audit ZM (Dev)",
            Triple("Apprenticeships", "Auto Mechanic Workshop Apprentice", 2400.0) to "Ndola Auto Care (Dev)",
            Triple("Freelance", "Architectural CAD Draftsperson (Remote/Hybrid)", 6500.0) to "BuildRight Studio (Dev)",
            Triple("Full-time", "Field Sales Representative (FMCG)", 5800.0) to "ZedFoods Distribution (Dev)",
            Triple("Gigs", "Weekend Event Sound & Lighting Technician", 1800.0) to "Lusaka Live Events (Dev)",
            Triple("Part-time", "Primary Mathematics After-School Tutor", 3000.0) to "Kansenshi Academy (Dev)",
            Triple("Casual", "Warehouse Packaging & Dispatch Assistants", 2600.0) to "Central Logistics (Dev)",
            Triple("Full-time", "Hospitality Front Desk Receptionist", 4900.0) to "Victoria Falls Lodge (Dev)"
        )

        val jobs = jobSpecs.mapIndexed { idx, (spec, employer) ->
            val (sub, title, salary) = spec
            val loc = locations[(idx + 1) % locations.size]
            Listing(
                id = "dev_job_${idx + 1}",
                ownerId = "dev_fixture_owner_${(idx % 5) + 30}",
                sellerName = employer,
                sellerAccountType = "business",
                sellerVerified = false,
                isPromoted = idx == 0 || idx == 4,
                expiresAt = if (idx == 0 || idx == 4) futureIso(10) else null,
                categoryId = "jobs",
                subcategory = sub,
                title = title,
                description = "$employer is hiring for $title in ${loc.displayFull}. Send CV and NRC copy via listed contact methods.",
                price = salary,
                priceMode = "monthly",
                location = loc,
                images = listOf(ListingImage(url = imgJob, thumbUrl = imgJob)),
                contactPreferences = contact(idx + 75),
                categoryDetails = CategoryDetails(
                    employmentType = sub,
                    employerName = employer
                ),
                moderationStatus = "live_reviewed",
                availabilityStatus = "available",
                isPublic = true,
                createdAt = isoDaysAgo((idx * 6 + 4).toLong()),
                publishedAt = isoDaysAgo((idx * 6 + 4).toLong())
            )
        }

        // 5. 15 Wanted Requests (PERMANENTLY TEXT-ONLY, ZERO IMAGES)
        val wantedSpecs = listOf(
            Triple("Products wanted", "Wanted: Clean iPhone 13 or 14 128GB", 11500.0),
            Triple("Property wanted", "Wanted: 2-Bedroom Flat to Rent in Woodlands", 6500.0),
            Triple("Products wanted", "Wanted: Used Lenovo ThinkPad i5 16GB RAM", 6800.0),
            Triple("Vehicles wanted", "Wanted: Clean Toyota Vitz or Honda Fit Automatic", 95000.0),
            Triple("Services wanted", "Wanted: Reliable Plumber for Borehole Booster Setup", 1500.0),
            Triple("Property wanted", "Wanted: Single Self-Contained Room to Rent in Kitwe", 2200.0),
            Triple("Products wanted", "Wanted: 5kVA Hybrid Inverter + 2 Lithium Batteries", 22000.0),
            Triple("Jobs wanted", "Wanted: Certified Class CE Driver Seeking Role", 7500.0),
            Triple("Products wanted", "Wanted: Commercial 3-Burner Gas Stove & Deep Fryer", 5500.0),
            Triple("Property wanted", "Wanted: Retail Shop Space to Rent Along Cairo Road", 8000.0),
            Triple("Services wanted", "Wanted: Graphic Designer for Company Profile Booklet", 1200.0),
            Triple("Products wanted", "Wanted: 6-Seater Dining Table Good Condition", 5000.0),
            Triple("Vehicles wanted", "Wanted: Clean Nissan X-Trail 2010–2013 Petrol", 130000.0),
            Triple("Products wanted", "Wanted: Sony PS5 Controller & FC25 Game Disc", 1800.0),
            Triple("Other requests", "Wanted: 2000L Water Storage Tank in Ndola", 2900.0)
        )

        val wanted = wantedSpecs.mapIndexed { idx, (sub, title, budget) ->
            val loc = locations[(idx + 3) % locations.size]
            Listing(
                id = "dev_wnt_${idx + 1}",
                ownerId = "dev_fixture_owner_${(idx % 6) + 40}",
                sellerName = "Requester Test ${(idx % 6) + 1} (Dev)",
                sellerVerified = false,
                isPromoted = false,
                categoryId = "wanted",
                subcategory = sub,
                title = title,
                description = "$title around ${loc.displayFull}. Budget is flexible for clean verified options — call or WhatsApp directly.",
                price = budget,
                priceMode = "fixed",
                location = loc,
                images = emptyList(), // Wanted is permanently text-only
                contactPreferences = contact(idx + 90),
                categoryDetails = CategoryDetails(
                    wantedDetails = "Ready buyer in ${loc.town} (${loc.district})"
                ),
                moderationStatus = "live_reviewed",
                availabilityStatus = "available",
                wantedStatus = "open",
                isPublic = true,
                createdAt = isoDaysAgo((idx * 4 + 2).toLong()),
                publishedAt = isoDaysAgo((idx * 4 + 2).toLong())
            )
        }

        // 6. 15 Swaps
        val swapSpecs = listOf(
            Triple("Products", "Swap: PlayStation 5 Digital + Cash for Laptop", 13500.0) to (imgGaming to "HP EliteBook or ThinkPad Core i7"),
            Triple("Products", "Swap: Samsung Galaxy S22 Ultra for iPhone 13 Pro", 13000.0) to (imgPhone to "iPhone 13 Pro / 14 Battery >=85%"),
            Triple("Vehicles", "Swap: 2011 Toyota Allion for Toyota Hilux Single Cab", 138000.0) to (imgCar to "Toyota Hilux / Isuzu Pickup + top-up"),
            Triple("Equipment", "Swap: Honda 6.5kVA Petrol Generator for Solar Inverter", 9500.0) to (imgTools to "5kVA Hybrid Solar Inverter"),
            Triple("Products", "Swap: MacBook Pro 2019 16-inch for Windows Workstation", 15000.0) to (imgLaptop to "RTX Laptop for Architecture CAD"),
            Triple("Products", "Swap: 50-inch LG Smart TV for Double Door Fridge", 6500.0) to (imgTv to "Defy or Hisense Double Door Fridge"),
            Triple("Equipment", "Swap: Bosch Rotary Hammer Drill + Grinder for Welder", 4200.0) to (imgTools to "Inverter Welding Machine + Helmet"),
            Triple("Products", "Swap: Canon EOS M50 Camera Kit for DJI Drone", 11500.0) to (imgDesign to "DJI Mini 3 / Mavic Air Combo"),
            Triple("Vehicles", "Swap: 2013 Nissan Note for Small Commercial Van", 82000.0) to (imgCar to "Nissan NV200 or Toyota Probox"),
            Triple("Products", "Swap: 6-Seater Leather Sofa for Queen Bed + Wardrobe", 9800.0) to (imgFurniture to "Queen Bed Frame + 4-Door Wardrobe"),
            Triple("Products", "Swap: Xiaomi 13T 256GB for iPad Air M1", 9200.0) to (imgPhone to "iPad Air M1 or Samsung Tab S8"),
            Triple("Equipment", "Swap: Commercial Popcorn & Slush Machine for Chest Freezer", 7400.0) to (imgAppliance to "300L+ Deep Chest Freezer"),
            Triple("Other exchanges", "Swap: Electric Treadmill Fitness Pro for Mountain Bike", 5800.0) to (imgTools to "29er Hydraulic Mountain Bike + Cash"),
            Triple("Products", "Swap: HP Color LaserJet Printer for 27-inch IPS Monitor", 4900.0) to (imgLaptop to "Dell / LG 27-inch QHD Monitor"),
            Triple("Products", "Swap: Xbox Series X 1TB for Gaming Desktop PC", 14200.0) to (imgGaming to "Core i5/Ryzen 5 GTX/RTX PC")
        )

        val swaps = swapSpecs.mapIndexed { idx, (spec, extra) ->
            val (sub, title, value) = spec
            val (imgUrl, swapFor) = extra
            val loc = locations[(idx + 5) % locations.size]
            Listing(
                id = "dev_swp_${idx + 1}",
                ownerId = "dev_fixture_owner_${(idx % 5) + 50}",
                sellerName = "Swapper Test ${(idx % 5) + 1} (Dev)",
                sellerVerified = false,
                isPromoted = idx == 0,
                expiresAt = if (idx == 0) futureIso(10) else null,
                categoryId = "swaps",
                subcategory = sub,
                title = title,
                description = "$title in ${loc.displayFull}. Looking to swap for: $swapFor. Inspection in public meeting spot.",
                price = value,
                priceMode = "fixed",
                location = loc,
                images = listOf(ListingImage(url = imgUrl, thumbUrl = imgUrl)),
                contactPreferences = contact(idx + 105),
                categoryDetails = CategoryDetails(
                    swapFor = swapFor,
                    condition = if (idx % 2 == 0) "Like New" else "Used - Good"
                ),
                moderationStatus = "live_reviewed",
                availabilityStatus = "available",
                isPublic = true,
                createdAt = isoDaysAgo((idx * 5 + 1).toLong()),
                publishedAt = isoDaysAgo((idx * 5 + 1).toLong())
            )
        }

        return products + rentals + services + jobs + wanted + swaps
    }
}
