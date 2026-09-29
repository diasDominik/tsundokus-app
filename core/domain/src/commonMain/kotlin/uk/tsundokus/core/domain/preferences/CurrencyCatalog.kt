package uk.tsundokus.core.domain.preferences

import kotlin.concurrent.Volatile

/**
 * The currencies the app offers. The server owns the list (/api/currencies), so a new currency
 * reaches every install without a release; [update] swaps it in once fetched, and the app caches it.
 * Until then — a first launch offline — the list built in below stands in: every currency in use
 * when this app was built (ISO 4217, from the JDK's ISO and CLDR data), made the same way the server
 * makes its list.
 *
 * Symbols are only used where no other currency shares them ("$" goes to the US dollar, "£" to the
 * pound); the rest show their code, since orders in several currencies sit side by side and "$ 12"
 * must not be ambiguous. `after` marks the currencies written after the amount ("12.99 €"), by how
 * most of the locales using them write it. Regions (ISO 3166 country codes) are for guessing a
 * default from the device.
 */
object CurrencyCatalog {
    private class Snapshot(
        val all: List<AppCurrency>,
        val byCode: Map<String, AppCurrency>,
        val byRegion: Map<String, AppCurrency>,
    )

    internal val all: List<AppCurrency> get() = snapshot.all

    internal val byCode: Map<String, AppCurrency> get() = snapshot.byCode

    internal val byRegion: Map<String, AppCurrency> get() = snapshot.byRegion

    /**
     * Makes [currencies] the offered list, with [regions] (currency code to ISO 3166 countries) for
     * guessing a default. Codes and regions the new list does not mention still resolve as before, so
     * an order in a dropped currency keeps its symbol. An empty list changes nothing.
     */
    fun update(
        currencies: List<AppCurrency>,
        regions: Map<String, List<String>>,
    ) {
        if (currencies.isEmpty()) return
        val byCode = currencies.associateBy(AppCurrency::code)
        snapshot =
            Snapshot(
                all = currencies,
                byCode = snapshot.byCode + byCode,
                byRegion =
                    snapshot.byRegion +
                        regions.flatMap { (code, countries) ->
                            val currency = byCode[code] ?: return@flatMap emptyList()
                            countries.map { it to currency }
                        },
            )
    }

    private class Entry(
        val currency: AppCurrency,
        val regions: String,
    )

    private fun c(
        code: String,
        symbol: String,
        decimals: Int,
        name: String,
        regions: String,
        after: Boolean = false,
    ) = Entry(AppCurrency(code, symbol, decimals, name, symbolAfterAmount = after), regions)

    private val BUNDLED =
        listOf(
            c("AED", "AED", 2, "United Arab Emirates Dirham", "AE"),
            c("AFN", "؋", 2, "Afghan Afghani", "AF"),
            c("ALL", "Lekë", 2, "Albanian Lek", "AL", after = true),
            c("AMD", "֏", 2, "Armenian Dram", "AM", after = true),
            c("AOA", "Kz", 2, "Angolan Kwanza", "AO", after = true),
            c("ARS", "ARS", 2, "Argentine Peso", "AR"),
            c("AUD", "A\$", 2, "Australian Dollar", "AU CC CX HM KI NF NR TV"),
            c("AWG", "Afl.", 2, "Aruban Florin", "AW"),
            c("AZN", "₼", 2, "Azerbaijani Manat", "AZ", after = true),
            c("BAM", "KM", 2, "Bosnia-Herzegovina Convertible Mark", "BA", after = true),
            c("BBD", "BBD", 2, "Barbadian Dollar", "BB"),
            c("BDT", "৳", 2, "Bangladeshi Taka", "BD", after = true),
            c("BGN", "лв.", 2, "Bulgarian Lev", "BG", after = true),
            c("BHD", "BHD", 3, "Bahraini Dinar", "BH", after = true),
            c("BIF", "FBu", 0, "Burundian Franc", "BI", after = true),
            c("BMD", "BMD", 2, "Bermudan Dollar", "BM"),
            c("BND", "BND", 2, "Brunei Dollar", "BN"),
            c("BOB", "Bs", 2, "Bolivian Boliviano", "BO"),
            c("BRL", "R\$", 2, "Brazilian Real", "BR"),
            c("BSD", "BSD", 2, "Bahamian Dollar", "BS"),
            c("BTN", "Nu.", 2, "Bhutanese Ngultrum", "BT"),
            c("BWP", "P", 2, "Botswanan Pula", "BW"),
            c("BYN", "BYN", 2, "Belarusian Ruble", "BY", after = true),
            c("BZD", "BZD", 2, "Belize Dollar", "BZ"),
            c("CAD", "CA\$", 2, "Canadian Dollar", "CA"),
            c("CDF", "FC", 2, "Congolese Franc", "CD", after = true),
            c("CHF", "CHF", 2, "Swiss Franc", "CH LI"),
            c("CLP", "CLP", 0, "Chilean Peso", "CL"),
            c("CNY", "CN¥", 2, "Chinese Yuan", "CN"),
            c("COP", "COP", 2, "Colombian Peso", "CO"),
            c("CRC", "₡", 2, "Costa Rican Colón", "CR"),
            c("CUP", "CUP", 2, "Cuban Peso", "CU"),
            c("CVE", "​", 2, "Cape Verdean Escudo", "CV", after = true),
            c("CZK", "Kč", 2, "Czech Koruna", "CZ", after = true),
            c("DJF", "Fdj", 0, "Djiboutian Franc", "DJ", after = true),
            c("DKK", "DKK", 2, "Danish Krone", "DK FO GL", after = true),
            c("DOP", "RD\$", 2, "Dominican Peso", "DO"),
            c("DZD", "DA", 2, "Algerian Dinar", "DZ", after = true),
            c("EGP", "EGP", 2, "Egyptian Pound", "EG", after = true),
            c("ERN", "Nfk", 2, "Eritrean Nakfa", "ER"),
            c("ETB", "ETB", 2, "Ethiopian Birr", "ET"),
            c(
                "EUR",
                "€",
                2,
                "Euro",
                "AD AT AX BE BL CY DE EE ES FI FR GF GP GR HR IE IT LT LU LV MC ME MF MQ MT NL PM PT RE SI SK SM TF VA YT",
                after = true,
            ),
            c("FJD", "FJD", 2, "Fijian Dollar", "FJ"),
            c("FKP", "FKP", 2, "Falkland Islands Pound", "FK"),
            c("GBP", "£", 2, "British Pound", "GB GG GS IM JE"),
            c("GEL", "₾", 2, "Georgian Lari", "GE"),
            c("GHS", "GH₵", 2, "Ghanaian Cedi", "GH"),
            c("GIP", "GIP", 2, "Gibraltar Pound", "GI"),
            c("GMD", "D", 2, "Gambian Dalasi", "GM"),
            c("GNF", "FG", 0, "Guinean Franc", "GN", after = true),
            c("GTQ", "Q", 2, "Guatemalan Quetzal", "GT"),
            c("GYD", "GYD", 2, "Guyanaese Dollar", "GY"),
            c("HKD", "HK\$", 2, "Hong Kong Dollar", "HK"),
            c("HNL", "HNL", 2, "Honduran Lempira", "HN"),
            c("HTG", "G", 2, "Haitian Gourde", "HT", after = true),
            c("HUF", "Ft", 2, "Hungarian Forint", "HU", after = true),
            c("IDR", "Rp", 2, "Indonesian Rupiah", "ID"),
            c("ILS", "₪", 2, "Israeli New Shekel", "IL PS", after = true),
            c("INR", "₹", 2, "Indian Rupee", "IN"),
            c("IQD", "IQD", 3, "Iraqi Dinar", "IQ", after = true),
            c("IRR", "ریال", 2, "Iranian Rial", "IR"),
            c("ISK", "kr.", 0, "Icelandic Króna", "IS", after = true),
            c("JMD", "JMD", 2, "Jamaican Dollar", "JM"),
            c("JOD", "JOD", 3, "Jordanian Dinar", "JO", after = true),
            c("JPY", "¥", 0, "Japanese Yen", "JP"),
            c("KES", "Ksh", 2, "Kenyan Shilling", "KE"),
            c("KGS", "сом", 2, "Kyrgystani Som", "KG", after = true),
            c("KHR", "៛", 2, "Cambodian Riel", "KH", after = true),
            c("KMF", "CF", 0, "Comorian Franc", "KM", after = true),
            c("KPW", "KPW", 2, "North Korean Won", "KP"),
            c("KRW", "₩", 0, "South Korean Won", "KR"),
            c("KWD", "KWD", 3, "Kuwaiti Dinar", "KW", after = true),
            c("KYD", "KYD", 2, "Cayman Islands Dollar", "KY"),
            c("KZT", "₸", 2, "Kazakhstani Tenge", "KZ", after = true),
            c("LAK", "₭", 2, "Laotian Kip", "LA"),
            c("LBP", "LBP", 2, "Lebanese Pound", "LB", after = true),
            c("LKR", "Rs.", 2, "Sri Lankan Rupee", "LK"),
            c("LRD", "LRD", 2, "Liberian Dollar", "LR"),
            c("LSL", "LSL", 2, "Lesotho Loti", "LS"),
            c("LYD", "LYD", 3, "Libyan Dinar", "LY", after = true),
            c("MAD", "MAD", 2, "Moroccan Dirham", "EH MA", after = true),
            c("MDL", "MDL", 2, "Moldovan Leu", "MD", after = true),
            c("MGA", "Ar", 2, "Malagasy Ariary", "MG"),
            c("MKD", "den", 2, "Macedonian Denar", "MK", after = true),
            c("MMK", "MMK", 2, "Myanmar Kyat", "MM", after = true),
            c("MNT", "₮", 2, "Mongolian Tugrik", "MN"),
            c("MOP", "MOP\$", 2, "Macanese Pataca", "MO"),
            c("MRU", "UM", 2, "Mauritanian Ouguiya", "MR", after = true),
            c("MUR", "MUR", 2, "Mauritian Rupee", "MU"),
            c("MVR", "Rf", 2, "Maldivian Rufiyaa", "MV"),
            c("MWK", "MK", 2, "Malawian Kwacha", "MW"),
            c("MXN", "MX\$", 2, "Mexican Peso", "MX"),
            c("MYR", "RM", 2, "Malaysian Ringgit", "MY"),
            c("MZN", "MTn", 2, "Mozambican Metical", "MZ", after = true),
            c("NAD", "NAD", 2, "Namibian Dollar", "NA"),
            c("NGN", "₦", 2, "Nigerian Naira", "NG"),
            c("NIO", "C\$", 2, "Nicaraguan Córdoba", "NI"),
            c("NOK", "NOK", 2, "Norwegian Krone", "BV NO SJ"),
            c("NPR", "नेरू", 2, "Nepalese Rupee", "NP"),
            c("NZD", "NZ\$", 2, "New Zealand Dollar", "CK NU NZ PN TK"),
            c("OMR", "OMR", 3, "Omani Rial", "OM", after = true),
            c("PAB", "B/.", 2, "Panamanian Balboa", "PA"),
            c("PEN", "S/", 2, "Peruvian Sol", "PE"),
            c("PGK", "PGK", 2, "Papua New Guinean Kina", "PG"),
            c("PHP", "₱", 2, "Philippine Peso", "PH"),
            c("PKR", "PKR", 2, "Pakistani Rupee", "PK"),
            c("PLN", "zł", 2, "Polish Zloty", "PL", after = true),
            c("PYG", "Gs.", 0, "Paraguayan Guarani", "PY"),
            c("QAR", "QAR", 2, "Qatari Riyal", "QA", after = true),
            c("RON", "RON", 2, "Romanian Leu", "RO", after = true),
            c("RSD", "RSD", 2, "Serbian Dinar", "RS", after = true),
            c("RUB", "₽", 2, "Russian Ruble", "RU", after = true),
            c("RWF", "RF", 0, "Rwandan Franc", "RW"),
            c("SAR", "SAR", 2, "Saudi Riyal", "SA", after = true),
            c("SBD", "SBD", 2, "Solomon Islands Dollar", "SB"),
            c("SCR", "SR", 2, "Seychellois Rupee", "SC"),
            c("SDG", "ج.س.", 2, "Sudanese Pound", "SD"),
            c("SEK", "SEK", 2, "Swedish Krona", "SE", after = true),
            c("SGD", "SGD", 2, "Singapore Dollar", "SG"),
            c("SHP", "SHP", 2, "St. Helena Pound", "SH"),
            c("SLE", "Le", 2, "Sierra Leonean Leone", "SL"),
            c("SOS", "S", 2, "Somali Shilling", "SO"),
            c("SRD", "SRD", 2, "Surinamese Dollar", "SR"),
            c("SSP", "SSP", 2, "South Sudanese Pound", "SS"),
            c("STN", "Db", 2, "São Tomé & Príncipe Dobra", "ST", after = true),
            c("SVC", "C", 2, "Salvadoran Colón", "SV"),
            c("SYP", "LS", 2, "Syrian Pound", "SY", after = true),
            c("SZL", "E", 2, "Swazi Lilangeni", "SZ"),
            c("THB", "฿", 2, "Thai Baht", "TH"),
            c("TJS", "сом.", 2, "Tajikistani Somoni", "TJ", after = true),
            c("TMT", "TMT", 2, "Turkmenistani Manat", "TM", after = true),
            c("TND", "DT", 3, "Tunisian Dinar", "TN", after = true),
            c("TOP", "T\$", 2, "Tongan Paʻanga", "TO"),
            c("TRY", "₺", 2, "Turkish Lira", "TR"),
            c("TTD", "TTD", 2, "Trinidad & Tobago Dollar", "TT"),
            c("TWD", "NT\$", 2, "New Taiwan Dollar", "TW"),
            c("TZS", "TSh", 2, "Tanzanian Shilling", "TZ"),
            c("UAH", "₴", 2, "Ukrainian Hryvnia", "UA", after = true),
            c("UGX", "USh", 0, "Ugandan Shilling", "UG"),
            c("USD", "\$", 2, "US Dollar", "AS BQ EC FM GU IO MH MP PR PW TC TL UM US VG VI"),
            c("UYU", "UYU", 2, "Uruguayan Peso", "UY"),
            c("UZS", "soʻm", 2, "Uzbekistani Som", "UZ", after = true),
            c("VES", "Bs.S", 2, "Venezuelan Bolívar", "VE"),
            c("VND", "₫", 0, "Vietnamese Dong", "VN", after = true),
            c("VUV", "VT", 0, "Vanuatu Vatu", "VU"),
            c("WST", "WS\$", 2, "Samoan Tala", "WS"),
            c("XAF", "FCFA", 0, "Central African CFA Franc", "CF CG CM GA GQ TD", after = true),
            c("XCD", "EC\$", 2, "East Caribbean Dollar", "AG AI DM GD KN LC MS VC"),
            c("XCG", "XCG", 2, "Caribbean Guilder", "CW SX"),
            c("XOF", "XOF", 0, "West African CFA Franc", "BF BJ CI GW ML NE SN TG", after = true),
            c("XPF", "CFPF", 0, "CFP Franc", "NC PF WF", after = true),
            c("YER", "YER", 2, "Yemeni Rial", "YE", after = true),
            c("ZAR", "R", 2, "South African Rand", "ZA"),
            c("ZMW", "ZMW", 2, "Zambian Kwacha", "ZM"),
            c("ZWG", "ZWG", 2, "Zimbabwe Gold", "ZW"),
        )

    // Declared after BUNDLED: object properties initialise in order.
    @Volatile
    private var snapshot: Snapshot =
        Snapshot(
            all = BUNDLED.map { it.currency },
            byCode = BUNDLED.associate { it.currency.code to it.currency },
            byRegion = BUNDLED.flatMap { entry -> entry.regions.split(' ').map { it to entry.currency } }.toMap(),
        )
}
