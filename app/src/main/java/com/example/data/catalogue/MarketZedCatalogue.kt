package com.example.data.catalogue

import com.example.data.model.CategoryOption
import com.example.data.model.Listing
import com.example.data.model.ProvinceLocations

object MarketZedCatalogue {

    const val DEFAULT_VERIFICATION_FEE_ZMW = 30.0
    const val OWNERSHIP_DECLARATION_VERSION = "2026-11-16-v1"
    const val OWNERSHIP_DECLARATION_TEXT =
        "I confirm that I am the lawful owner of this item, property, or service, or that I have lawful authority to publish this listing on MarketZed. All details and photos I have provided are accurate and comply with Zambian law and MarketZed Terms."

    private val landTermsRegex = Regex(
        "\\b(land|lands|plot|plots|acre|acres|hectare|hectares|farmland|smallholding|title\\s*deed|leasehold\\s*land|vacant\\s*land|residential\\s*plot|commercial\\s*plot)\\b",
        RegexOption.IGNORE_CASE
    )

    private val propertySaleTermsRegex = Regex(
        "\\b(for\\s*sale|house\\s*for\\s*sale|home\\s*for\\s*sale|property\\s*for\\s*sale|building\\s*for\\s*sale|shop\\s*for\\s*sale|apartment\\s*for\\s*sale|selling\\s*(?:this|my|a)?\\s*(?:house|home|apartment|building|shop|property))\\b",
        RegexOption.IGNORE_CASE
    )

    val categories: List<CategoryOption> = listOf(
        CategoryOption(
            id = "property",
            name = "Property",
            description = "Homes, apartments, rooms, offices and commercial spaces for rent",
            subcategories = listOf(
                "Houses", "Apartments", "Rooms", "Short stays",
                "Shops", "Offices", "Commercial property", "Warehouses"
            ),
            requiresImage = true
        ),
        CategoryOption(
            id = "marketplace",
            name = "Marketplace",
            description = "Discover products and everyday essentials",
            subcategories = listOf(
                "Phones and accessories", "Computers and laptops", "TVs and electronics",
                "Gaming", "Furniture", "Clothing and fashion", "Home appliances",
                "Vehicles", "Vehicle spare parts", "Building materials", "Other goods"
            ),
            requiresImage = true
        ),
        CategoryOption(
            id = "swaps",
            name = "Swaps",
            description = "Exchange items and explore offers",
            subcategories = listOf(
                "Products", "Vehicles", "Equipment", "Other exchanges"
            ),
            requiresImage = true
        ),
        CategoryOption(
            id = "free",
            name = "Free / Give Away",
            description = "Find useful things being given away",
            subcategories = listOf(
                "Furniture", "Clothing", "Electronics", "Household items",
                "Books", "Equipment", "Other"
            ),
            requiresImage = true
        ),
        CategoryOption(
            id = "services",
            name = "Services",
            description = "Find trusted local skills and professionals",
            subcategories = listOf(
                "Plumbing", "Electrical", "Carpentry", "Mechanics", "Cleaning",
                "Painting", "Graphic design", "Web development", "Photography",
                "Tutoring", "Transport", "Phone repair", "Catering", "Other services"
            ),
            requiresImage = true
        ),
        CategoryOption(
            id = "jobs",
            name = "Jobs & Opportunities",
            description = "Explore work, internships and gigs",
            subcategories = listOf(
                "Full-time", "Part-time", "Temporary", "Casual",
                "Freelance", "Internships", "Apprenticeships", "Gigs"
            ),
            requiresImage = false
        ),
        CategoryOption(
            id = "wanted",
            name = "Wanted",
            description = "Tell Zambia what you are looking for",
            subcategories = listOf(
                "Property wanted", "Products wanted", "Vehicles wanted",
                "Services wanted", "Jobs wanted", "Other requests"
            ),
            requiresImage = false
        )
    )

    val homeShortcutCategoryIds: List<String> = listOf(
        "property", "marketplace", "services", "swaps", "free"
    )

    val provinces: List<ProvinceLocations> = listOf(
        ProvinceLocations("Lusaka", listOf("Lusaka", "Chilanga", "Chongwe", "Kafue", "Luangwa", "Rufunsa")),
        ProvinceLocations("Copperbelt", listOf("Kitwe", "Ndola", "Chingola", "Mufulira", "Luanshya", "Kalulushi", "Chililabombwe")),
        ProvinceLocations("Central", listOf("Kabwe", "Kapiri Mposhi", "Mkushi", "Mumbwa", "Serenje", "Chibombo")),
        ProvinceLocations("Southern", listOf("Livingstone", "Choma", "Mazabuka", "Monze", "Kalomo", "Siavonga")),
        ProvinceLocations("Eastern", listOf("Chipata", "Katete", "Lundazi", "Petauke", "Nyimba")),
        ProvinceLocations("Northern", listOf("Kasama", "Mbala", "Mpulungu", "Luwingu", "Mungwi")),
        ProvinceLocations("Luapula", listOf("Mansa", "Samfya", "Nchelenge", "Kawambwa")),
        ProvinceLocations("Muchinga", listOf("Chinsali", "Mpika", "Nakonde", "Isoka")),
        ProvinceLocations("North-Western", listOf("Solwezi", "Kasempa", "Mwinilunga", "Zambezi", "Kalumbila")),
        ProvinceLocations("Western", listOf("Mongu", "Senanga", "Kaoma", "Sesheke", "Kalabo"))
    )

    val conditionOptions = listOf("New", "Like New", "Used - Good", "Used - Fair", "Refurbished")
    val employmentTypeOptions = listOf("Full-time", "Part-time", "Contract", "Temporary", "Freelance", "Internship", "Apprenticeship", "Gig")
    val reportReasons = listOf(
        "suspected_fraud" to "Suspected fraud or scam",
        "stolen_goods" to "Suspected stolen goods",
        "counterfeit" to "Counterfeit item",
        "misleading" to "Misleading or inaccurate details",
        "prohibited" to "Prohibited item or service",
        "other" to "Other concern"
    )

    val districtsByProvince: Map<String, List<String>> = mapOf(
        "Lusaka" to listOf("Lusaka District", "Chilanga District", "Chongwe District", "Kafue District", "Luangwa District", "Rufunsa District"),
        "Copperbelt" to listOf("Kitwe District", "Ndola District", "Chingola District", "Mufulira District", "Luanshya District", "Kalulushi District", "Chililabombwe District", "Lufwanyama District", "Masaiti District", "Mpongwe District"),
        "Central" to listOf("Kabwe District", "Kapiri Mposhi District", "Mkushi District", "Mumbwa District", "Serenje District", "Chibombo District", "Chisamba District"),
        "Southern" to listOf("Livingstone District", "Choma District", "Mazabuka District", "Monze District", "Kalomo District", "Siavonga District", "Namwala District", "Sinazongwe District"),
        "Eastern" to listOf("Chipata District", "Katete District", "Lundazi District", "Petauke District", "Nyimba District", "Mambwe District"),
        "Northern" to listOf("Kasama District", "Mbala District", "Mpulungu District", "Luwingu District", "Mungwi District"),
        "Luapula" to listOf("Mansa District", "Samfya District", "Nchelenge District", "Kawambwa District", "Mwense District"),
        "Muchinga" to listOf("Chinsali District", "Mpika District", "Nakonde District", "Isoka District", "Mafinga District"),
        "North-Western" to listOf("Solwezi District", "Kasempa District", "Mwinilunga District", "Zambezi District", "Kalumbila District"),
        "Western" to listOf("Mongu District", "Senanga District", "Kaoma District", "Sesheke District", "Kalabo District")
    )

    val allDistricts: List<String> by lazy {
        districtsByProvince.values.flatten().distinct()
    }

    fun categoryById(id: String?): CategoryOption? =
        categories.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }

    fun categoryLabel(id: String?): String =
        categoryById(id)?.name ?: id?.replaceFirstChar { it.uppercase() } ?: "Listing"

    fun townsForProvince(province: String?): List<String> =
        if (province.isNullOrBlank()) provinces.flatMap { it.towns }.distinct()
        else provinces.firstOrNull { it.province.equals(province.trim(), ignoreCase = true) }?.towns.orEmpty()

    fun districtsForProvince(province: String?): List<String> =
        if (province.isNullOrBlank()) allDistricts
        else districtsByProvince.entries
            .firstOrNull { it.key.equals(province.trim(), ignoreCase = true) }
            ?.value
            .orEmpty()

    /**
     * Enforces MarketZed's non-negotiable property policy:
     * - Land/plots (sales, rentals, or leases) are completely prohibited.
     * - Property listings are strictly FOR RENT ONLY; property-for-sale is excluded.
     */
    fun validatePropertyAndLandPolicy(
        categoryId: String?,
        subcategory: String?,
        title: String?,
        description: String?,
        priceMode: String? = null
    ): String? {
        val normCat = categoryId?.trim()?.lowercase().orEmpty()
        val normSub = subcategory?.trim().orEmpty()
        val combinedText = listOfNotNull(subcategory, title, description).joinToString(" ")

        if (normCat == "land" || normSub.equals("land", ignoreCase = true) || normSub.equals("plots", ignoreCase = true)) {
            return "Land and plot listings are not allowed on MarketZed. Only houses, apartments, rooms, shops, offices, and commercial spaces for rent are permitted."
        }

        if (normCat == "property") {
            if (landTermsRegex.containsMatchIn(combinedText)) {
                return "Land and plots are excluded from MarketZed. Property listings must be homes, rooms, shops, offices, or warehouses for rent."
            }
            if (priceMode.equals("sale", ignoreCase = true) || propertySaleTermsRegex.containsMatchIn(combinedText)) {
                return "Property listings on MarketZed are FOR RENT ONLY. Property-for-sale listings are not permitted."
            }
        }

        if (normCat == "wanted" && normSub.equals("Property wanted", ignoreCase = true) && landTermsRegex.containsMatchIn(combinedText)) {
            return "Land and plot requests are excluded on MarketZed. Wanted property requests must be for rentals only."
        }

        return null
    }

    fun isProhibitedLandOrPropertySale(
        categoryId: String?,
        subcategory: String?,
        title: String?,
        description: String?,
        priceMode: String? = null
    ): Boolean = validatePropertyAndLandPolicy(categoryId, subcategory, title, description, priceMode) != null

    fun isProhibitedLandOrPropertySale(listing: Listing): Boolean =
        isProhibitedLandOrPropertySale(
            categoryId = listing.categoryId,
            subcategory = listing.subcategory,
            title = listing.title,
            description = listing.description,
            priceMode = listing.priceMode
        )

    data class PopularProductSubcategory(
        val id: String,
        val label: String,
        val categoryId: String,
        val subcategoryFilter: String
    )

    val popularProductSubcategories: List<PopularProductSubcategory> = listOf(
        PopularProductSubcategory("phones", "Phones", "marketplace", "Phones and accessories"),
        PopularProductSubcategory("laptops", "Laptops", "marketplace", "Computers and laptops"),
        PopularProductSubcategory("tvs", "TVs & Audio", "marketplace", "TVs and electronics"),
        PopularProductSubcategory("furniture", "Furniture", "marketplace", "Furniture"),
        PopularProductSubcategory("appliances", "Appliances", "marketplace", "Home appliances"),
        PopularProductSubcategory("vehicles", "Vehicles", "marketplace", "Vehicles"),
        PopularProductSubcategory("fashion", "Fashion", "marketplace", "Clothing and fashion")
    )

    val comparisonSpecKeysBySubcategory: Map<String, List<String>> = mapOf(
        "Phones and accessories" to listOf("Brand", "Model", "RAM", "Storage", "Battery", "Condition"),
        "Computers and laptops" to listOf("Brand", "Processor", "RAM", "Storage", "Screen size", "Condition"),
        "TVs and electronics" to listOf("Brand", "Screen size", "Resolution", "Smart TV", "Condition"),
        "Furniture" to listOf("Type", "Material", "Dimensions", "Condition"),
        "Home appliances" to listOf("Brand", "Model", "Power / Capacity", "Condition"),
        "Vehicles" to listOf("Brand", "Model", "Year", "Transmission", "Fuel", "Mileage", "Condition")
    )

    fun expectedSpecKeysForListing(listing: Listing): List<String> {
        val cat = listing.categoryId.trim().lowercase()
        val sub = listing.subcategory.trim()
        return when {
            cat == "property" -> listOf("Property type", "Bedrooms", "Lease type")
            cat == "swaps" -> listOf("Swap for", "Condition")
            comparisonSpecKeysBySubcategory.containsKey(sub) -> comparisonSpecKeysBySubcategory[sub].orEmpty()
            else -> listOf("Brand", "Model", "Condition")
        }
    }

    /**
     * Extracts category-aware specifications using ONLY recorded listing data.
     * Missing attributes always return "Not provided" and are never invented.
     */
    fun extractComparisonSpecs(
        listing: Listing,
        keysOverride: List<String>? = null
    ): LinkedHashMap<String, String> {
        val recorded = listing.resolvedSpecifications
        val keys = (keysOverride ?: expectedSpecKeysForListing(listing))
        val result = linkedMapOf<String, String>()

        fun findValue(key: String): String {
            recorded.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            return when (key.lowercase()) {
                "condition" -> listing.categoryDetails?.condition?.takeIf { it.isNotBlank() } ?: "Not provided"
                "brand" -> listing.categoryDetails?.brand?.takeIf { it.isNotBlank() } ?: "Not provided"
                "model" -> listing.categoryDetails?.model?.takeIf { it.isNotBlank() } ?: "Not provided"
                "property type" -> listing.subcategory.takeIf { it.isNotBlank() } ?: "Not provided"
                "bedrooms" -> listing.categoryDetails?.bedrooms?.takeIf { it.isNotBlank() } ?: "Not provided"
                "lease type" -> if (listing.categoryId.equals("property", ignoreCase = true)) "For Rent Only" else "Not provided"
                "swap for" -> listing.categoryDetails?.swapFor?.takeIf { it.isNotBlank() } ?: "Not provided"
                "type" -> listing.subcategory.takeIf { it.isNotBlank() } ?: "Not provided"
                else -> "Not provided"
            }
        }

        keys.forEach { key ->
            result[key] = findValue(key)
        }
        // Append any additional seller-provided spec keys not already included
        recorded.forEach { (k, v) ->
            if (result.keys.none { it.equals(k, ignoreCase = true) } && v.isNotBlank()) {
                result[k] = v
            }
        }
        return result
    }
}
