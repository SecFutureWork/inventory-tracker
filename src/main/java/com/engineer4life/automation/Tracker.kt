package com.engineer4life.automation

import com.microsoft.playwright.Browser
import org.jsoup.Jsoup

data class RangeData<E>(val first : E, val last: E)

data class HouseInfo(
    var floorPlanName: String,
    var minStory: Int,
    var maxStory: Int,
    var minBdr: Double,
    var maxBdr: Double,
    var minFullBath: Double,
    var maxFullBath: Double,
    var minHalfBath: Double?,
    var maxHalfBath: Double?,
    var minGarageSize: Double,
    var maxGarageSize: Double,
    var minLotSize: Int?,
    var maxLotSize: Int?,
    var minPrice: Double,
    var maxPrice: Double,
    var isFixedPrice: Boolean = true,
    var isAcreage: Boolean = false,
    var status: CommunityStatus
    ){
    constructor(floorPlanName: String) : this(floorPlanName, 0, 0,
        0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0, 0,
        0.0, 0.0, false, false, CommunityStatus.UNAVAILABLE)

    fun stripSpace(data: String): String {
        return data.strip()
    }

    fun stripDollarSign(data: String): String {
        return data.trimStart('$')
    }

    fun stripCommaSymbol(data: String): String {
        return data.replace(",", "")
    }

    fun stripExcessFat(range: String, prefix: String = "", suffix: String =""): String{
        return range.trimStart(*prefix.toCharArray()).trimEnd(*suffix.toCharArray())
    }

    fun setStories(storyRange: String, trimPrefix: String){
        val (first, last) = extractRange(storyRange, trimPrefix)
        minStory = first.toInt()
        maxStory = last.toInt()
    }

    fun setBdr(bdrRange: String, trimPrefix: String){
        val (first, last) = extractRange(bdrRange, trimPrefix)
        minBdr = first.toDouble()
        maxBdr = last.toDouble()
    }

    fun setLotSize(lotSizeRange: String, trimPrefix: String){
        val (first, last) = extractRange(lotSizeRange, trimPrefix)
        minLotSize = first.toInt()
        maxLotSize = last.toInt()
    }

    fun setFullBath(fullBathRange: String, trimPrefix: String){
        val (first, last) = extractRange(fullBathRange, trimPrefix)
        minFullBath = first.toDouble()
        maxFullBath = last.toDouble()
    }

    fun setHalfBath(halfBathRange: String, trimPrefix: String){
        val (first, last) = extractRange(halfBathRange, trimPrefix)
        minHalfBath = first.toDouble()
        maxHalfBath = last.toDouble()
    }

    fun setGarage(garageRange: String, trimPrefix: String, trimSuffix: String=""){
        val (first, last) = extractRange(garageRange, trimPrefix, trimSuffix)
        minGarageSize = first.toDouble()
        maxGarageSize = last.toDouble()
    }

    fun setPrice(priceRange: String){
        val (first, last) = extractRange(priceRange)
        minPrice = first.toDouble()
        maxPrice = last.toDouble()
    }

    fun extractRange(range: String, prefix: String="", suffix: String=""): RangeData<String> {
        val _range =  stripExcessFat(range, prefix, suffix).split("-")
        when (_range.size){
            1 -> {
                val sqft = stripDollarSign(stripCommaSymbol(stripSpace(_range[0])))
                return RangeData<String>(sqft, sqft)
            }
            else -> {
                val minRange = stripDollarSign(stripCommaSymbol(stripSpace(_range[0])))
                val maxRange = stripDollarSign(stripCommaSymbol(stripSpace(_range[1])))
                return RangeData<String>(minRange, maxRange)
            }
        }
    }
}

enum class CommunityStatus {
    MODELS_AVAILABLE_TO_TOUR, COMING_SOON, NOW_SELLING, UNAVAILABLE
}

interface HouseTracker {
    fun scrape(url: String): List<HouseInfo>
    fun write(houseInfoList: List<HouseInfo>)
}

class DreesHomes(val browser: Browser): HouseTracker{
    override fun scrape(url: String): List<HouseInfo> {
        val page = browser.newPage()
        page.navigate(url)
        val floorPlans = page.locator("div.home-cards").innerHTML()
        val floorPlansDoc = Jsoup.parseBodyFragment(floorPlans)

        val houseListInfo = mutableListOf<HouseInfo>()
        for(floorPlanDoc in floorPlansDoc.getElementsByClass("HomeCard"))
        {
            val titleDoc = floorPlanDoc.getElementsByClass("name-price").first()
            var floorPlanName = titleDoc.getElementsByTag("a").first().ownText()
            var priceRange = titleDoc.getElementsByClass("price").first()
                .getElementsByTag("span").first().ownText()
            var sqft: String
            if(floorPlanDoc.getElementsByClass("neighborhood-feature sqftr").first() != null)
                sqft = floorPlanDoc.getElementsByClass("neighborhood-feature sqftr").first().ownText()
            else
                sqft = floorPlanDoc.getElementsByClass("neighborhood-feature sqft").first().ownText()
            val stories = floorPlanDoc.getElementsByClass("neighborhood-feature stories").first().ownText()
            val bdrs = floorPlanDoc.getElementsByClass("neighborhood-feature bdrooms").first().ownText()
            var fullBaths = floorPlanDoc.getElementsByClass("neighborhood-feature f_baths").first().ownText()
            var halfBaths: String = "0"
            if(floorPlanDoc.getElementsByClass("neighborhood-feature h_baths").first() != null)
                halfBaths = floorPlanDoc.getElementsByClass("neighborhood-feature h_baths").first().ownText()
            val garages = floorPlanDoc.getElementsByClass("neighborhood-feature garage").first().ownText()
//            println("${floorPlanName}, ${priceRange}, ${sqft}, ${stories}, ${bdrs}, ${fullBaths}, ${garages}")

            val house = HouseInfo(floorPlanName)
            house.setBdr(bdrs.ifEmpty { "0" }, "Bedrooms: ")
            house.setGarage(garages.ifEmpty { "0" }, trimPrefix = "Garage: ", "-car")
            house.setPrice(priceRange.ifEmpty{"0"})
            house.setFullBath(fullBaths.ifEmpty{"0"},"Full Baths: ")
            house.setHalfBath(halfBaths.ifEmpty{"0"},"Half Baths: ")
            house.setStories(stories.ifEmpty{"0"},"Stories: ")
            house.setLotSize(sqft.ifEmpty { "0" }, "Square Feet: ")
            houseListInfo.add(house)
        }
        page.close()
        return houseListInfo
    }

    fun scrape(url: String, communityName: String): List<HouseInfo> {
        return scrape(url)
    }

    override fun write(houseInfoList: List<HouseInfo>) {
        TODO("Not yet implemented")
    }

}

class TaylorMorrisonHouseTracker(val browser: Browser): HouseTracker {
    override fun scrape(url: String): List<HouseInfo> {
        val page = browser.newPage()
        page.navigate(url)

        page.locator("button.promo-modal-close").first().click()

        page.locator("text=SELECT A STATE").click()
        page.locator("text=Texas").last().click()
        page.locator("text=Search Homes ").last().click()

        val communities = page.locator("div.search-page-community-card")
        println(communities.innerHTML())
        var communitiesGroupDoc = Jsoup.parse(communities.innerHTML())
        val container = communitiesGroupDoc.select("div.container")
        for(element in container){
            val communityGroup = element.getElementsByTag("h4").last()
            val city = element.getElementsByTag("p").first()
            val numCommunities = element.getElementsByTag("p")[1]
            println("${communityGroup.ownText()}, ${city.ownText()}, ${numCommunities.ownText()}")

            val communitiesDoc = element.getElementsByClass("col-lg-6 search-page-community-card__community-group-card-content-col").parents().first().children()
            for(communityDoc in communitiesDoc){
                val communityName = communityDoc.getElementsByClass("community-card__description-community-name").first().ownText()
                val communityStatus = communityDoc.getElementsByClass("community-card__description-community-status").first().ownText()
                val communityPrice = communityDoc.getElementsByClass("community-card__community-details-price").first().ownText()
                val communitySqftRange = communityDoc.getElementsByClass("community-card__community-details-figure").first().ownText()

                val communityDetailLink = communityDoc.getElementsByClass("community-card__community-detail-link").first().attr("href")
                scrapeCommunityDetail("${url}${communityDetailLink}")
                println("\t ${communityName}, ${communityStatus}, ${communityPrice}, ${communitySqftRange}" )
//                \n\t\t ${communityDetailLink}")
            }


        }

        return listOf<HouseInfo>()
    }

    fun scrapeCommunityDetail(url: String): List<HouseInfo>{
        var houseInfoList = mutableListOf<HouseInfo>()
        val page = browser.newPage()
        page.navigate(url)

        page.locator("button.promo-modal-close").first().click()

        val gallery = page.locator("div#slickA").first().innerHTML()
        val galleryDoc = Jsoup.parse(gallery)
        val floorPlanDocs = galleryDoc.getElementsByClass("community-gallery__info-box")
        for(floorPlanDoc in floorPlanDocs){
            val name = floorPlanDoc.allElements.first().getElementsByClass("community-gallery__item-title")
            println(name)
        }
//        println(gallery)
//        val communityGallery = page.locator("div.community-gallery__info.box").first().innerHTML()
//        val communityGalleryDoc = Jsoup.parse(communityGallery)
        return houseInfoList
    }

    override fun write(houseInfoList: List<HouseInfo>) {

    }
}