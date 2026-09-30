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

    private fun currency(
        code: String,
        symbol: String,
        decimals: Int,
        name: String,
        regions: String,
        after: Boolean = false,
    ) = Entry(AppCurrency(code, symbol, decimals, name, symbolAfterAmount = after), regions)

    private val BUNDLED =
        listOf(
            currency("AED", "AED", 2, "United Arab Emirates Dirham", "AE"),
            currency("AFN", "؋", 2, "Afghan Afghani", "AF"),
            currency("ALL", "Lekë", 2, "Albanian Lek", "AL", after = true),
            currency("AMD", "֏", 2, "Armenian Dram", "AM", after = true),
            currency("AOA", "Kz", 2, "Angolan Kwanza", "AO", after = true),
            currency("ARS", "ARS", 2, "Argentine Peso", "AR"),
            currency("AUD", "A\$", 2, "Australian Dollar", "AU CC CX HM KI NF NR TV"),
            currency("AWG", "Afl.", 2, "Aruban Florin", "AW"),
            currency("AZN", "₼", 2, "Azerbaijani Manat", "AZ", after = true),
            currency("BAM", "KM", 2, "Bosnia-Herzegovina Convertible Mark", "BA", after = true),
            currency("BBD", "BBD", 2, "Barbadian Dollar", "BB"),
            currency("BDT", "৳", 2, "Bangladeshi Taka", "BD", after = true),
            currency("BGN", "лв.", 2, "Bulgarian Lev", "BG", after = true),
            currency("BHD", "BHD", 3, "Bahraini Dinar", "BH", after = true),
            currency("BIF", "FBu", 0, "Burundian Franc", "BI", after = true),
            currency("BMD", "BMD", 2, "Bermudan Dollar", "BM"),
            currency("BND", "BND", 2, "Brunei Dollar", "BN"),
            currency("BOB", "Bs", 2, "Bolivian Boliviano", "BO"),
            currency("BRL", "R\$", 2, "Brazilian Real", "BR"),
            currency("BSD", "BSD", 2, "Bahamian Dollar", "BS"),
            currency("BTN", "Nu.", 2, "Bhutanese Ngultrum", "BT"),
            currency("BWP", "P", 2, "Botswanan Pula", "BW"),
            currency("BYN", "BYN", 2, "Belarusian Ruble", "BY", after = true),
            currency("BZD", "BZD", 2, "Belize Dollar", "BZ"),
            currency("CAD", "CA\$", 2, "Canadian Dollar", "CA"),
            currency("CDF", "FC", 2, "Congolese Franc", "CD", after = true),
            currency("CHF", "CHF", 2, "Swiss Franc", "CH LI"),
            currency("CLP", "CLP", 0, "Chilean Peso", "CL"),
            currency("CNY", "CN¥", 2, "Chinese Yuan", "CN"),
            currency("COP", "COP", 2, "Colombian Peso", "CO"),
            currency("CRC", "₡", 2, "Costa Rican Colón", "CR"),
            currency("CUP", "CUP", 2, "Cuban Peso", "CU"),
            currency("CVE", "​", 2, "Cape Verdean Escudo", "CV", after = true),
            currency("CZK", "Kč", 2, "Czech Koruna", "CZ", after = true),
            currency("DJF", "Fdj", 0, "Djiboutian Franc", "DJ", after = true),
            currency("DKK", "DKK", 2, "Danish Krone", "DK FO GL", after = true),
            currency("DOP", "RD\$", 2, "Dominican Peso", "DO"),
            currency("DZD", "DA", 2, "Algerian Dinar", "DZ", after = true),
            currency("EGP", "EGP", 2, "Egyptian Pound", "EG", after = true),
            currency("ERN", "Nfk", 2, "Eritrean Nakfa", "ER"),
            currency("ETB", "ETB", 2, "Ethiopian Birr", "ET"),
            currency(
                "EUR",
                "€",
                2,
                "Euro",
                "AD AT AX BE BL CY DE EE ES FI FR GF GP GR HR IE IT LT LU LV MC ME MF MQ MT NL PM PT RE SI SK SM TF VA YT",
                after = true,
            ),
            currency("FJD", "FJD", 2, "Fijian Dollar", "FJ"),
            currency("FKP", "FKP", 2, "Falkland Islands Pound", "FK"),
            currency("GBP", "£", 2, "British Pound", "GB GG GS IM JE"),
            currency("GEL", "₾", 2, "Georgian Lari", "GE"),
            currency("GHS", "GH₵", 2, "Ghanaian Cedi", "GH"),
            currency("GIP", "GIP", 2, "Gibraltar Pound", "GI"),
            currency("GMD", "D", 2, "Gambian Dalasi", "GM"),
            currency("GNF", "FG", 0, "Guinean Franc", "GN", after = true),
            currency("GTQ", "Q", 2, "Guatemalan Quetzal", "GT"),
            currency("GYD", "GYD", 2, "Guyanaese Dollar", "GY"),
            currency("HKD", "HK\$", 2, "Hong Kong Dollar", "HK"),
            currency("HNL", "HNL", 2, "Honduran Lempira", "HN"),
            currency("HTG", "G", 2, "Haitian Gourde", "HT", after = true),
            currency("HUF", "Ft", 2, "Hungarian Forint", "HU", after = true),
            currency("IDR", "Rp", 2, "Indonesian Rupiah", "ID"),
            currency("ILS", "₪", 2, "Israeli New Shekel", "IL PS", after = true),
            currency("INR", "₹", 2, "Indian Rupee", "IN"),
            currency("IQD", "IQD", 3, "Iraqi Dinar", "IQ", after = true),
            currency("IRR", "ریال", 2, "Iranian Rial", "IR"),
            currency("ISK", "kr.", 0, "Icelandic Króna", "IS", after = true),
            currency("JMD", "JMD", 2, "Jamaican Dollar", "JM"),
            currency("JOD", "JOD", 3, "Jordanian Dinar", "JO", after = true),
            currency("JPY", "¥", 0, "Japanese Yen", "JP"),
            currency("KES", "Ksh", 2, "Kenyan Shilling", "KE"),
            currency("KGS", "сом", 2, "Kyrgystani Som", "KG", after = true),
            currency("KHR", "៛", 2, "Cambodian Riel", "KH", after = true),
            currency("KMF", "CF", 0, "Comorian Franc", "KM", after = true),
            currency("KPW", "KPW", 2, "North Korean Won", "KP"),
            currency("KRW", "₩", 0, "South Korean Won", "KR"),
            currency("KWD", "KWD", 3, "Kuwaiti Dinar", "KW", after = true),
            currency("KYD", "KYD", 2, "Cayman Islands Dollar", "KY"),
            currency("KZT", "₸", 2, "Kazakhstani Tenge", "KZ", after = true),
            currency("LAK", "₭", 2, "Laotian Kip", "LA"),
            currency("LBP", "LBP", 2, "Lebanese Pound", "LB", after = true),
            currency("LKR", "Rs.", 2, "Sri Lankan Rupee", "LK"),
            currency("LRD", "LRD", 2, "Liberian Dollar", "LR"),
            currency("LSL", "LSL", 2, "Lesotho Loti", "LS"),
            currency("LYD", "LYD", 3, "Libyan Dinar", "LY", after = true),
            currency("MAD", "MAD", 2, "Moroccan Dirham", "EH MA", after = true),
            currency("MDL", "MDL", 2, "Moldovan Leu", "MD", after = true),
            currency("MGA", "Ar", 2, "Malagasy Ariary", "MG"),
            currency("MKD", "den", 2, "Macedonian Denar", "MK", after = true),
            currency("MMK", "MMK", 2, "Myanmar Kyat", "MM", after = true),
            currency("MNT", "₮", 2, "Mongolian Tugrik", "MN"),
            currency("MOP", "MOP\$", 2, "Macanese Pataca", "MO"),
            currency("MRU", "UM", 2, "Mauritanian Ouguiya", "MR", after = true),
            currency("MUR", "MUR", 2, "Mauritian Rupee", "MU"),
            currency("MVR", "Rf", 2, "Maldivian Rufiyaa", "MV"),
            currency("MWK", "MK", 2, "Malawian Kwacha", "MW"),
            currency("MXN", "MX\$", 2, "Mexican Peso", "MX"),
            currency("MYR", "RM", 2, "Malaysian Ringgit", "MY"),
            currency("MZN", "MTn", 2, "Mozambican Metical", "MZ", after = true),
            currency("NAD", "NAD", 2, "Namibian Dollar", "NA"),
            currency("NGN", "₦", 2, "Nigerian Naira", "NG"),
            currency("NIO", "C\$", 2, "Nicaraguan Córdoba", "NI"),
            currency("NOK", "NOK", 2, "Norwegian Krone", "BV NO SJ"),
            currency("NPR", "नेरू", 2, "Nepalese Rupee", "NP"),
            currency("NZD", "NZ\$", 2, "New Zealand Dollar", "CK NU NZ PN TK"),
            currency("OMR", "OMR", 3, "Omani Rial", "OM", after = true),
            currency("PAB", "B/.", 2, "Panamanian Balboa", "PA"),
            currency("PEN", "S/", 2, "Peruvian Sol", "PE"),
            currency("PGK", "PGK", 2, "Papua New Guinean Kina", "PG"),
            currency("PHP", "₱", 2, "Philippine Peso", "PH"),
            currency("PKR", "PKR", 2, "Pakistani Rupee", "PK"),
            currency("PLN", "zł", 2, "Polish Zloty", "PL", after = true),
            currency("PYG", "Gs.", 0, "Paraguayan Guarani", "PY"),
            currency("QAR", "QAR", 2, "Qatari Riyal", "QA", after = true),
            currency("RON", "RON", 2, "Romanian Leu", "RO", after = true),
            currency("RSD", "RSD", 2, "Serbian Dinar", "RS", after = true),
            currency("RUB", "₽", 2, "Russian Ruble", "RU", after = true),
            currency("RWF", "RF", 0, "Rwandan Franc", "RW"),
            currency("SAR", "SAR", 2, "Saudi Riyal", "SA", after = true),
            currency("SBD", "SBD", 2, "Solomon Islands Dollar", "SB"),
            currency("SCR", "SR", 2, "Seychellois Rupee", "SC"),
            currency("SDG", "ج.س.", 2, "Sudanese Pound", "SD"),
            currency("SEK", "SEK", 2, "Swedish Krona", "SE", after = true),
            currency("SGD", "SGD", 2, "Singapore Dollar", "SG"),
            currency("SHP", "SHP", 2, "St. Helena Pound", "SH"),
            currency("SLE", "Le", 2, "Sierra Leonean Leone", "SL"),
            currency("SOS", "S", 2, "Somali Shilling", "SO"),
            currency("SRD", "SRD", 2, "Surinamese Dollar", "SR"),
            currency("SSP", "SSP", 2, "South Sudanese Pound", "SS"),
            currency("STN", "Db", 2, "São Tomé & Príncipe Dobra", "ST", after = true),
            currency("SVC", "C", 2, "Salvadoran Colón", "SV"),
            currency("SYP", "LS", 2, "Syrian Pound", "SY", after = true),
            currency("SZL", "E", 2, "Swazi Lilangeni", "SZ"),
            currency("THB", "฿", 2, "Thai Baht", "TH"),
            currency("TJS", "сом.", 2, "Tajikistani Somoni", "TJ", after = true),
            currency("TMT", "TMT", 2, "Turkmenistani Manat", "TM", after = true),
            currency("TND", "DT", 3, "Tunisian Dinar", "TN", after = true),
            currency("TOP", "T\$", 2, "Tongan Paʻanga", "TO"),
            currency("TRY", "₺", 2, "Turkish Lira", "TR"),
            currency("TTD", "TTD", 2, "Trinidad & Tobago Dollar", "TT"),
            currency("TWD", "NT\$", 2, "New Taiwan Dollar", "TW"),
            currency("TZS", "TSh", 2, "Tanzanian Shilling", "TZ"),
            currency("UAH", "₴", 2, "Ukrainian Hryvnia", "UA", after = true),
            currency("UGX", "USh", 0, "Ugandan Shilling", "UG"),
            currency("USD", "\$", 2, "US Dollar", "AS BQ EC FM GU IO MH MP PR PW TC TL UM US VG VI"),
            currency("UYU", "UYU", 2, "Uruguayan Peso", "UY"),
            currency("UZS", "soʻm", 2, "Uzbekistani Som", "UZ", after = true),
            currency("VES", "Bs.S", 2, "Venezuelan Bolívar", "VE"),
            currency("VND", "₫", 0, "Vietnamese Dong", "VN", after = true),
            currency("VUV", "VT", 0, "Vanuatu Vatu", "VU"),
            currency("WST", "WS\$", 2, "Samoan Tala", "WS"),
            currency("XAF", "FCFA", 0, "Central African CFA Franc", "CF CG CM GA GQ TD", after = true),
            currency("XCD", "EC\$", 2, "East Caribbean Dollar", "AG AI DM GD KN LC MS VC"),
            currency("XCG", "XCG", 2, "Caribbean Guilder", "CW SX"),
            currency("XOF", "XOF", 0, "West African CFA Franc", "BF BJ CI GW ML NE SN TG", after = true),
            currency("XPF", "CFPF", 0, "CFP Franc", "NC PF WF", after = true),
            currency("YER", "YER", 2, "Yemeni Rial", "YE", after = true),
            currency("ZAR", "R", 2, "South African Rand", "ZA"),
            currency("ZMW", "ZMW", 2, "Zambian Kwacha", "ZM"),
            currency("ZWG", "ZWG", 2, "Zimbabwe Gold", "ZW"),
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
