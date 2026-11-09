package com.d2m.app.ui.strings

/**
 * WeddingChecklistScreen and its sheets (ui/screens/shared/wedding/). Task titles, phase names and panchangam names
 * (tithi / nakshatra) are server content and arrive in English; everything the app itself says is here. Hindi and Tamil
 * are first-pass translations -- worth a native speaker's read before release.
 */
interface WeddingStrings {
    // screen
    val loading: String
    val notAvailableTitle: String
    val notAvailableBody: String
    val noTasksTitle: String
    val noTasksBody: String
    val clearFilters: String
    val loadError: String
    val actionError: String
    // controls
    val layoutList: String
    val layoutBoard: String
    val layoutCalendar: String
    val filtersButton: String
    fun filtersButtonCount(n: Int): String
    val searchPlaceholder: String
    fun groupBy(name: String): String
    val groupPhase: String
    val groupDue: String
    val groupOwner: String
    val groupParty: String
    val noParty: String
    val newTaskButton: String
    val addInGroup: String
    fun moreNarrow(n: Int): String
    fun percentDone(pct: Int): String
    fun daysToWedding(n: Int): String
    fun daysSinceWedding(n: Int): String
    val setKeyDatesHint: String
    fun chipOverdue(n: Int): String
    fun chipThisWeek(n: Int): String
    fun chipMine(n: Int): String
    val keyDates: String
    fun tasksOfTotal(shown: Int, total: Int): String
    // names
    val me: String
    val bride: String
    val groom: String
    val brideParents: String
    val groomParents: String
    fun parentsOf(name: String): String
    val sharedDecisions: String
    // type / buckets / quick views / visibility
    val scopeIndividual: String
    val scopeCouple: String
    val scopeParents: String
    val scopeFamily: String
    val scopeGroup: String
    val scopeShared: String
    val bucketOverdue: String
    val bucketNext7: String
    val bucketNext30: String
    val bucketLater: String
    val bucketNoDate: String
    val bucketDone: String
    val quickMine: String
    val quickOverdue: String
    val quickWeek: String
    val quickJoint: String
    val visEveryone: String
    val visMyFamily: String
    val visOnlyMe: String
    // dates
    val relToday: String
    val relTomorrow: String
    val relYesterday: String
    fun relDaysOverdue(n: Int): String
    fun relInDays(n: Int): String
    val noDate: String
    val agoJustNow: String
    fun agoMinutes(n: Int): String
    fun agoHours(n: Int): String
    fun agoDays(n: Int): String
    val monthsShort: List<String>
    val monthsLong: List<String>
    val weekdaysShort: List<String>
    val weekdaysLong: List<String>
    // calendar
    val calToday: String
    val calWedding: String
    fun dueThisMonth(n: Int): String
    fun withoutDate(n: Int): String
    val engagement: String
    val marriage: String
    val honeymoon: String
    val engagementShort: String
    val weddingShort: String
    val honeymoonShort: String
    // panchangam
    val pmMuhoortham: String
    val pmPurnima: String
    val pmAmavasya: String
    val pmChandrashtamam: String
    val pmShubh: String
    val pmAmavasyaAvoid: String
    fun pmChandrashtamamFor(names: String): String
    val pmChecking: String
    // task sheet
    val taskFallback: String
    val markDone: String
    val markAgreed: String
    val doneReopen: String
    fun assignedOnly(names: String): String
    fun signedOffBy(names: String): String
    fun withParty(party: String): String
    val consult: String
    fun commentsPill(n: Int): String
    fun filesPill(n: Int): String
    val assigned: String
    val everyone: String
    val bothParents: String
    fun nameAndParents(name: String): String
    fun nPeople(n: Int): String
    val addTodoTitle: String
    val addTodoIntro: String
    val ownersHint: String
    val anyOwnerCanMark: String
    val whenLabel: String
    val chipToday: String
    val chipInWeek: String
    val chipInMonth: String
    val noDateChip: String
    val visibilityEveryoneHint: String
    val visibilitySideHint: String
    val visibilityOnlyMeHint: String
    val addTodoButton: String
    val milestoneEngagement: String
    val milestoneMarriage: String
    val milestoneHoneymoon: String
    val engagementBeforeWedding: String
    val honeymoonAfterWedding: String
    fun daysBeforeWedding(n: Int): String
    fun daysAfterWedding(n: Int): String
    fun inDays(n: Int): String
    fun daysAgo(n: Int): String
    val saveDates: String
    val traditionHint: String
    val ordinaryDay: String
    val panchangam: String
    val pmMuhoortham1: String
    val pmPurnima1: String
    val pmAmavasya1: String
    val pmChandra1: String
    val todosOnThisDay: String
    val chooseFromList: String
    val searchYourTodos: String
    fun nowDueOn(date: String): String
    val noDateYet: String
    val moveToThisDay: String
    val nothingMatches: String
    val closeAction: String
    fun doneOfTotal(done: Int, total: Int): String
    val creatorSuffix: String
    val signedOff: String
    val pending: String
    val due: String
    val setDate: String
    val clear: String
    val resetDate: String
    val saveTitle: String
    val titlePlaceholder: String
    val visibleTo: String
    val anchorCustom: String
    fun anchorOn(anchor: String): String
    fun anchorBefore(days: Int, anchor: String): String
    fun anchorAfter(days: Int, anchor: String): String
    val anchorEngagement: String
    val anchorMarriage: String
    val anchorHoneymoon: String
    val setKeyDatesToSchedule: String
    val attachments: String
    val addFile: String
    val uploading: String
    val attachmentsHint: String
    val opening: String
    val openWith: String
    val eventsOnThisDay: String
    val happeningOn: String
    val kindTodo: String
    val kindEvent: String
    val addEventTitle: String
    val addEventIntro: String
    val addEventButton: String
    val eventNote: String
    val addEventHere: String
    fun tipInvolves(party: String): String
    fun tipComments(n: Int): String
    fun tipFiles(n: Int): String
    val homeEyebrow: String
    val homeSetDates: String
    fun progressOf(done: Int, total: Int): String
    val nextUp: String
    val openChecklist: String
    val tapToOpen: String
    val remove: String
    val comments: String
    val earlierNote: String
    val noComments: String
    val writeComment: String
    val mentionSomeone: String
    fun laneAdd(lane: String): String
    val commentButton: String
    val posting: String
    val delete: String
    val removeTask: String
    val removeTaskTitle: String
    val removeTaskBody: String
    val cancel: String
    fun overLimit(name: String): String
    fun noAppCanOpen(name: String): String
    fun couldntOpen(name: String): String
    fun couldntUpload(name: String): String
    val couldntLoadComments: String
    val couldntPost: String
    val couldntDeleteComment: String
    val couldntRemoveFile: String
    // day sheet
    val nothingDue: String
    fun tasksSummary(n: Int, done: Int): String
    val addOnThisDay: String
    // new task
    val newTaskTitle: String
    val whatNeedsDoing: String
    val assignTo: String
    val anyoneCanMark: String
    val phase: String
    val dueDate: String
    val createTask: String
    // key dates dialog
    val keyDatesIntro: String
    val tradition: String
    val keyDatesRule: String
    val save: String
    val ok: String
    val traditionNames: Map<String, String>
    // filters sheet
    val filtersTitle: String
    val clearAll: String
    fun matchesCount(shown: Int, total: Int): String
    val quickViews: String
    val facetAssignedTo: String
    val facetType: String
    val facetPhase: String
    val facetStatus: String
    val statusTodo: String
    val statusDone: String
    val include: String
    val exclude: String
    fun showTasks(n: Int): String
}

object WeddingStringsEn : WeddingStrings {
    override val loading = "Loading…"
    override val notAvailableTitle = "Not available yet"
    override val notAvailableBody = "The checklist opens once your child's profile is linked."
    override val noTasksTitle = "No to-dos match"
    override val noTasksBody = "Try different filters."
    override val clearFilters = "Clear filters"
    override val loadError = "Couldn't load the wedding checklist."
    override val actionError = "That didn't work. Please try again."
    override val layoutList = "List"
    override val layoutBoard = "Board"
    override val layoutCalendar = "Calendar"
    override val filtersButton = "Filters"
    override fun filtersButtonCount(n: Int) = "Filters ($n)"
    override val searchPlaceholder = "Search to-dos…"
    override fun groupBy(name: String) = "Group: $name ▾"
    override val groupPhase = "Phase"
    override val groupDue = "Due date"
    override val groupOwner = "Person"
    override val groupParty = "Vendor / expert"
    override val noParty = "Just the families"
    override val newTaskButton = "+ New"
    override val addInGroup = "+ Add"
    override fun moreNarrow(n: Int) = "+$n more — narrow it with filters"
    override fun percentDone(pct: Int) = "$pct% done"
    override fun daysToWedding(n: Int) = "$n days to the wedding"
    override fun daysSinceWedding(n: Int) = "$n days since the wedding"
    override val setKeyDatesHint = "Set key dates"
    override fun chipOverdue(n: Int) = "Overdue $n"
    override fun chipThisWeek(n: Int) = "This week $n"
    override fun chipMine(n: Int) = "Mine $n"
    override val keyDates = "Key dates"
    override fun tasksOfTotal(shown: Int, total: Int) = "$shown of $total to-dos"
    override val me = "Me"
    override val bride = "Bride"
    override val groom = "Groom"
    override val brideParents = "Bride's parents"
    override val groomParents = "Groom's parents"
    override fun parentsOf(name: String) = "$name's parents"
    override val sharedDecisions = "Shared decisions"
    override val scopeIndividual = "Individual"
    override val scopeCouple = "Bride & groom"
    override val scopeParents = "Both parents"
    override val scopeFamily = "Within family"
    override val scopeGroup = "All four"
    override val scopeShared = "Shared"
    override val bucketOverdue = "Overdue"
    override val bucketNext7 = "Next 7 days"
    override val bucketNext30 = "Next 30 days"
    override val bucketLater = "Later"
    override val bucketNoDate = "No date"
    override val bucketDone = "Done"
    override val quickMine = "My open to-dos"
    override val quickOverdue = "Overdue"
    override val quickWeek = "Due in the next 7 days"
    override val quickJoint = "Joint decisions"
    override val visEveryone = "Everyone"
    override val visMyFamily = "My family only"
    override val visOnlyMe = "Only me"
    override val relToday = "Today"
    override val relTomorrow = "Tomorrow"
    override val relYesterday = "Yesterday"
    override fun relDaysOverdue(n: Int) = "${n}d overdue"
    override fun relInDays(n: Int) = "In $n days"
    override val noDate = "No date"
    override val agoJustNow = "just now"
    override fun agoMinutes(n: Int) = "${n}m ago"
    override fun agoHours(n: Int) = "${n}h ago"
    override fun agoDays(n: Int) = "${n}d ago"
    override val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    override val monthsLong = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    override val weekdaysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    override val weekdaysLong = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    override val calToday = "Today"
    override val calWedding = "Wedding"
    override fun dueThisMonth(n: Int) = "$n to-do${if (n == 1) "" else "s"} due this month"
    override fun withoutDate(n: Int) = "$n without a date"
    override val engagement = "Engagement"
    override val marriage = "Marriage"
    override val honeymoon = "Honeymoon"
    override val engagementShort = "Eng"
    override val weddingShort = "Wed"
    override val honeymoonShort = "Hon"
    override val pmMuhoortham = "Muhoortham"
    override val pmPurnima = "Purnima"
    override val pmAmavasya = "Amavasya"
    override val pmChandrashtamam = "Chandrashtamam"
    override val pmShubh = "Shubh muhoortham"
    override val pmAmavasyaAvoid = "Amavasya — usually avoided"
    override fun pmChandrashtamamFor(names: String) = "Chandrashtamam — $names"
    override val pmChecking = "Checking panchangam…"
    override val taskFallback = "To-do"
    override val markDone = "Mark as done"
    override val markAgreed = "Mark as agreed / done"
    override val doneReopen = "✓ Done — reopen"
    override fun assignedOnly(names: String) = "Only $names can mark this done"
    override fun signedOffBy(names: String) = "Signed off by $names"
    override fun withParty(party: String) = "with $party"
    override val consult = "Consult"
    override fun commentsPill(n: Int) = if (n == 1) "1 comment" else "$n comments"
    override fun filesPill(n: Int) = if (n == 1) "1 file" else "$n files"
    override val assigned = "Who's on it"
    override val everyone = "Everyone"
    override val bothParents = "Both parents"
    override fun nameAndParents(name: String) = "$name & parents"
    override fun nPeople(n: Int) = "$n people"
    override val addTodoTitle = "Add a to-do"
    override val addTodoIntro = "Something your families need to do, decide or arrange."
    override val ownersHint = "Pick everyone who should take this on"
    override val anyOwnerCanMark = "Anyone on it can mark it done for everyone."
    override val whenLabel = "When"
    override val chipToday = "Today"
    override val chipInWeek = "In a week"
    override val chipInMonth = "In a month"
    override val noDateChip = "No date"
    override val visibilityEveryoneHint = "Both families can see it."
    override val visibilitySideHint = "Only your own family can see it."
    override val visibilityOnlyMeHint = "Only you can see it."
    override val addTodoButton = "Add to-do"
    override val milestoneEngagement = "The betrothal ceremony"
    override val milestoneMarriage = "The muhurtham day — the day before and after are Marriage too"
    override val milestoneHoneymoon = "Time away together"
    override val engagementBeforeWedding = "The engagement has to be on or before the wedding."
    override val honeymoonAfterWedding = "The honeymoon has to be on or after the wedding."
    override fun daysBeforeWedding(n: Int) = "$n day${if (n == 1) "" else "s"} before the wedding"
    override fun daysAfterWedding(n: Int) = "$n day${if (n == 1) "" else "s"} after the wedding"
    override fun inDays(n: Int) = if (n == 0) "today" else "in $n day${if (n == 1) "" else "s"}"
    override fun daysAgo(n: Int) = "$n day${if (n == 1) "" else "s"} ago"
    override val saveDates = "Save dates"
    override val traditionHint = "Hides to-dos that belong to other Brahmin traditions."
    override val ordinaryDay = "No special markers — an ordinary day."
    override val panchangam = "Panchangam"
    override val pmMuhoortham1 = "A favourable day for new beginnings."
    override val pmPurnima1 = "Full moon."
    override val pmAmavasya1 = "New moon — usually avoided for ceremonies."
    override val pmChandra1 = "The Moon is in the 8th sign from their birth rashi."
    override val todosOnThisDay = "To-dos on this day"
    override val chooseFromList = "Choose from your list"
    override val searchYourTodos = "Search your to-dos…"
    override fun nowDueOn(date: String) = "Now due $date"
    override val noDateYet = "No date yet"
    override val moveToThisDay = "move it to this day"
    override val nothingMatches = "Nothing matches."
    override val closeAction = "Close"
    override fun doneOfTotal(done: Int, total: Int) = "$done of $total done"
    override val creatorSuffix = " · creator"
    override val signedOff = "signed off"
    override val pending = "pending"
    override val due = "Due"
    override val setDate = "Set date"
    override val clear = "Clear"
    override val resetDate = "Reset to calculated date"
    override val saveTitle = "Save title"
    override val titlePlaceholder = "To-do title"
    override val visibleTo = "Visible to"
    override val anchorCustom = "Custom date"
    override fun anchorOn(anchor: String) = "On the $anchor date"
    override fun anchorBefore(days: Int, anchor: String) = "$days days before the $anchor date"
    override fun anchorAfter(days: Int, anchor: String) = "$days days after the $anchor date"
    override val anchorEngagement = "engagement"
    override val anchorMarriage = "marriage"
    override val anchorHoneymoon = "honeymoon"
    override val setKeyDatesToSchedule = "Set the key dates to schedule this"
    override val attachments = "Attachments"
    override val addFile = "+ Add file"
    override val uploading = "Uploading…"
    override val attachmentsHint = "Quotes, photos, PDFs — both families can see them."
    override val opening = "Opening…"
    override val openWith = "Open with…"
    override val eventsOnThisDay = "Events on this day"
    override val happeningOn = "Happening on"
    override val kindTodo = "To-do"
    override val kindEvent = "Event"
    override val addEventTitle = "Add an event"
    override val addEventIntro = "A ceremony or gathering on a date — it shows on the calendar, not in the to-do list."
    override val addEventButton = "Add event"
    override val eventNote = "Event — an occasion on this date, nothing to tick off"
    override val addEventHere = "+ Add an event"
    override fun tipInvolves(party: String) = "Involves $party"
    override fun tipComments(n: Int) = if (n == 1) "1 comment" else "$n comments"
    override fun tipFiles(n: Int) = if (n == 1) "1 attachment" else "$n attachments"
    override val homeEyebrow = "Getting married soon"
    override val homeSetDates = "Set the key dates to start the countdown"
    override fun progressOf(done: Int, total: Int) = "$done of $total to-dos done"
    override val nextUp = "Next up"
    override val openChecklist = "Open checklist"
    override val tapToOpen = "Tap a document to open it."
    override val remove = "Remove"
    override val comments = "Comments"
    override val earlierNote = "Earlier note"
    override val noComments = "No comments yet. Start the conversation — both families can see it."
    override val writeComment = "Write a comment… use @ to mention someone"
    override val mentionSomeone = "Mention someone"
    override fun laneAdd(lane: String) = "Add a to-do to $lane"
    override val commentButton = "Comment"
    override val posting = "Posting…"
    override val delete = "Delete"
    override val removeTask = "Remove to-do"
    override val removeTaskTitle = "Remove this to-do?"
    override val removeTaskBody = "It disappears for everyone."
    override val cancel = "Cancel"
    override fun overLimit(name: String) = "$name is over 8 MB."
    override fun noAppCanOpen(name: String) = "No app on this device can open $name."
    override fun couldntOpen(name: String) = "Couldn't open $name."
    override fun couldntUpload(name: String) = "Couldn't upload $name."
    override val couldntLoadComments = "Couldn't load comments."
    override val couldntPost = "Couldn't post the comment."
    override val couldntDeleteComment = "Couldn't delete the comment."
    override val couldntRemoveFile = "Couldn't remove the file."
    override val nothingDue = "Nothing is due this day"
    override fun tasksSummary(n: Int, done: Int) = "$n to-do${if (n == 1) "" else "s"} · $done done"
    override val addOnThisDay = "+ Add a new to-do"
    override val newTaskTitle = "New to-do"
    override val whatNeedsDoing = "What needs to be done?"
    override val assignTo = "Who's on it"
    override val anyoneCanMark = "Anyone you pick can mark it done."
    override val phase = "Phase"
    override val dueDate = "Due date"
    override val createTask = "Create to-do"
    override val keyDatesIntro = "Every to-do's due date is worked out from these. Change one and the whole plan reschedules."
    override val tradition = "Tradition"
    override val keyDatesRule = "Engagement must be on or before the marriage, and the honeymoon on or after it."
    override val save = "Save"
    override val ok = "OK"
    override val traditionNames = mapOf(
        "" to "All Brahmin traditions", "tamil" to "Tamil (Iyer / Iyengar)", "telugu" to "Telugu", "kannada" to "Kannada (Smarta / Madhwa)",
        "malayali" to "Malayali (Nambudiri / Iyer)", "north" to "North Indian", "bengali" to "Bengali", "marathi" to "Marathi",
        "gujarati" to "Gujarati", "odia" to "Odia",
    )
    override val filtersTitle = "Filters"
    override val clearAll = "Clear all"
    override fun matchesCount(shown: Int, total: Int) = "$shown of $total to-dos match"
    override val quickViews = "Quick views"
    override val facetAssignedTo = "Who's on it"
    override val facetType = "Type"
    override val facetPhase = "Phase"
    override val facetStatus = "Status"
    override val statusTodo = "To do"
    override val statusDone = "Done"
    override val include = "Include"
    override val exclude = "Exclude"
    override fun showTasks(n: Int) = "Show $n to-dos"
}

object WeddingStringsHi : WeddingStrings {
    override val loading = "लोड हो रहा है…"
    override val notAvailableTitle = "अभी उपलब्ध नहीं"
    override val notAvailableBody = "आपके बच्चे की प्रोफ़ाइल जुड़ने पर चेकलिस्ट खुलेगी।"
    override val noTasksTitle = "कोई काम नहीं मिला"
    override val noTasksBody = "दूसरे फ़िल्टर आज़माएँ।"
    override val clearFilters = "फ़िल्टर हटाएँ"
    override val loadError = "विवाह चेकलिस्ट लोड नहीं हो सकी।"
    override val actionError = "यह नहीं हो सका। कृपया फिर कोशिश करें।"
    override val layoutList = "सूची"
    override val layoutBoard = "बोर्ड"
    override val layoutCalendar = "कैलेंडर"
    override val filtersButton = "फ़िल्टर"
    override fun filtersButtonCount(n: Int) = "फ़िल्टर ($n)"
    override val searchPlaceholder = "काम खोजें…"
    override fun groupBy(name: String) = "समूह: $name ▾"
    override val groupPhase = "चरण"
    override val groupDue = "नियत तिथि"
    override val groupOwner = "व्यक्ति"
    override val groupParty = "वेंडर / विशेषज्ञ"
    override val noParty = "सिर्फ़ परिवार"
    override val newTaskButton = "+ नया"
    override val addInGroup = "+ जोड़ें"
    override fun moreNarrow(n: Int) = "+$n और — फ़िल्टर से सीमित करें"
    override fun percentDone(pct: Int) = "$pct% पूर्ण"
    override fun daysToWedding(n: Int) = "विवाह में $n दिन"
    override fun daysSinceWedding(n: Int) = "विवाह को $n दिन हुए"
    override val setKeyDatesHint = "मुख्य तिथियाँ तय करें"
    override fun chipOverdue(n: Int) = "विलंबित $n"
    override fun chipThisWeek(n: Int) = "इस सप्ताह $n"
    override fun chipMine(n: Int) = "मेरे $n"
    override val keyDates = "मुख्य तिथियाँ"
    override fun tasksOfTotal(shown: Int, total: Int) = "$total में से $shown काम"
    override val me = "मैं"
    override val bride = "वधू"
    override val groom = "वर"
    override val brideParents = "वधू के माता-पिता"
    override val groomParents = "वर के माता-पिता"
    override fun parentsOf(name: String) = "$name के माता-पिता"
    override val sharedDecisions = "साझा निर्णय"
    override val scopeIndividual = "व्यक्तिगत"
    override val scopeCouple = "वधू और वर"
    override val scopeParents = "दोनों के माता-पिता"
    override val scopeFamily = "परिवार के भीतर"
    override val scopeGroup = "सभी चार"
    override val scopeShared = "साझा"
    override val bucketOverdue = "विलंबित"
    override val bucketNext7 = "अगले 7 दिन"
    override val bucketNext30 = "अगले 30 दिन"
    override val bucketLater = "बाद में"
    override val bucketNoDate = "कोई तिथि नहीं"
    override val bucketDone = "पूर्ण"
    override val quickMine = "मेरे खुले काम"
    override val quickOverdue = "विलंबित"
    override val quickWeek = "अगले 7 दिनों में देय"
    override val quickJoint = "संयुक्त निर्णय"
    override val visEveryone = "सभी"
    override val visMyFamily = "सिर्फ़ मेरा परिवार"
    override val visOnlyMe = "सिर्फ़ मैं"
    override val relToday = "आज"
    override val relTomorrow = "कल"
    override val relYesterday = "बीता कल"
    override fun relDaysOverdue(n: Int) = "$n दिन विलंबित"
    override fun relInDays(n: Int) = "$n दिन में"
    override val noDate = "कोई तिथि नहीं"
    override val agoJustNow = "अभी"
    override fun agoMinutes(n: Int) = "$n मिनट पहले"
    override fun agoHours(n: Int) = "$n घंटे पहले"
    override fun agoDays(n: Int) = "$n दिन पहले"
    override val monthsShort = listOf("जन॰", "फ़र॰", "मार्च", "अप्रै॰", "मई", "जून", "जुला॰", "अग॰", "सित॰", "अक्टू॰", "नव॰", "दिस॰")
    override val monthsLong = listOf("जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
    override val weekdaysShort = listOf("सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि", "रवि")
    override val weekdaysLong = listOf("सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार", "रविवार")
    override val calToday = "आज"
    override val calWedding = "विवाह"
    override fun dueThisMonth(n: Int) = "इस माह $n काम देय"
    override fun withoutDate(n: Int) = "$n बिना तिथि"
    override val engagement = "सगाई"
    override val marriage = "विवाह"
    override val honeymoon = "हनीमून"
    override val engagementShort = "सगाई"
    override val weddingShort = "विवाह"
    override val honeymoonShort = "हनीमून"
    override val pmMuhoortham = "मुहूर्त"
    override val pmPurnima = "पूर्णिमा"
    override val pmAmavasya = "अमावस्या"
    override val pmChandrashtamam = "चंद्राष्टम"
    override val pmShubh = "शुभ मुहूर्त"
    override val pmAmavasyaAvoid = "अमावस्या — आमतौर पर वर्जित"
    override fun pmChandrashtamamFor(names: String) = "चंद्राष्टम — $names"
    override val pmChecking = "पंचांग देखा जा रहा है…"
    override val taskFallback = "काम"
    override val markDone = "पूर्ण करें"
    override val markAgreed = "सहमत / पूर्ण करें"
    override val doneReopen = "✓ पूर्ण — फिर खोलें"
    override fun assignedOnly(names: String) = "केवल $names इसे पूर्ण कर सकते हैं"
    override fun signedOffBy(names: String) = "$names ने पूर्ण किया"
    override fun withParty(party: String) = "$party के साथ"
    override val consult = "परामर्श"
    override fun commentsPill(n: Int) = "$n टिप्पणी"
    override fun filesPill(n: Int) = "$n फ़ाइल"
    override val assigned = "कौन-कौन शामिल"
    override val everyone = "सभी"
    override val bothParents = "दोनों के माता-पिता"
    override fun nameAndParents(name: String) = "$name और माता-पिता"
    override fun nPeople(n: Int) = "$n लोग"
    override val addTodoTitle = "काम जोड़ें"
    override val addTodoIntro = "कुछ ऐसा जो आपके परिवारों को करना, तय करना या जुटाना है।"
    override val ownersHint = "जो इसे संभालें, उन सभी को चुनें"
    override val anyOwnerCanMark = "इसमें शामिल कोई भी एक इसे सबके लिए पूर्ण कर सकता है।"
    override val whenLabel = "कब"
    override val chipToday = "आज"
    override val chipInWeek = "एक सप्ताह में"
    override val chipInMonth = "एक माह में"
    override val noDateChip = "तिथि नहीं"
    override val visibilityEveryoneHint = "दोनों परिवार इसे देख सकते हैं।"
    override val visibilitySideHint = "केवल आपका परिवार इसे देख सकता है।"
    override val visibilityOnlyMeHint = "केवल आप इसे देख सकते हैं।"
    override val addTodoButton = "काम जोड़ें"
    override val milestoneEngagement = "सगाई की रस्म"
    override val milestoneMarriage = "मुहूर्त का दिन — उससे पहले और बाद का दिन भी विवाह में गिने जाते हैं"
    override val milestoneHoneymoon = "साथ बिताया समय"
    override val engagementBeforeWedding = "सगाई विवाह से पहले या उसी दिन होनी चाहिए।"
    override val honeymoonAfterWedding = "हनीमून विवाह के बाद या उसी दिन होना चाहिए।"
    override fun daysBeforeWedding(n: Int) = "विवाह से $n दिन पहले"
    override fun daysAfterWedding(n: Int) = "विवाह के $n दिन बाद"
    override fun inDays(n: Int) = if (n == 0) "आज" else "$n दिन में"
    override fun daysAgo(n: Int) = "$n दिन पहले"
    override val saveDates = "तिथियाँ सहेजें"
    override val traditionHint = "दूसरी ब्राह्मण परंपराओं के काम छिपा देता है।"
    override val ordinaryDay = "कोई विशेष चिह्न नहीं — एक सामान्य दिन।"
    override val panchangam = "पंचांग"
    override val pmMuhoortham1 = "नई शुरुआत के लिए अनुकूल दिन।"
    override val pmPurnima1 = "पूर्ण चंद्र।"
    override val pmAmavasya1 = "अमावस्या — समारोहों के लिए आमतौर पर वर्जित।"
    override val pmChandra1 = "चंद्रमा उनकी जन्म राशि से आठवें स्थान पर है।"
    override val todosOnThisDay = "इस दिन के काम"
    override val chooseFromList = "अपनी सूची से चुनें"
    override val searchYourTodos = "अपने काम खोजें…"
    override fun nowDueOn(date: String) = "अभी $date को देय"
    override val noDateYet = "अभी कोई तिथि नहीं"
    override val moveToThisDay = "इसे इस दिन ले आएँ"
    override val nothingMatches = "कुछ नहीं मिला।"
    override val closeAction = "बंद करें"
    override fun doneOfTotal(done: Int, total: Int) = "$total में से $done पूर्ण"
    override val creatorSuffix = " · निर्माता"
    override val signedOff = "पूर्ण किया"
    override val pending = "लंबित"
    override val due = "नियत तिथि"
    override val setDate = "तिथि चुनें"
    override val clear = "हटाएँ"
    override val resetDate = "गणना की गई तिथि पर लौटें"
    override val saveTitle = "शीर्षक सहेजें"
    override val titlePlaceholder = "काम का शीर्षक"
    override val visibleTo = "किसे दिखे"
    override val anchorCustom = "अपनी चुनी तिथि"
    override fun anchorOn(anchor: String) = "$anchor की तिथि पर"
    override fun anchorBefore(days: Int, anchor: String) = "$anchor की तिथि से $days दिन पहले"
    override fun anchorAfter(days: Int, anchor: String) = "$anchor की तिथि के $days दिन बाद"
    override val anchorEngagement = "सगाई"
    override val anchorMarriage = "विवाह"
    override val anchorHoneymoon = "हनीमून"
    override val setKeyDatesToSchedule = "इसे निर्धारित करने के लिए मुख्य तिथियाँ तय करें"
    override val attachments = "संलग्नक"
    override val addFile = "+ फ़ाइल जोड़ें"
    override val uploading = "अपलोड हो रहा है…"
    override val attachmentsHint = "कोटेशन, फ़ोटो, PDF — दोनों परिवार देख सकते हैं।"
    override val opening = "खुल रहा है…"
    override val openWith = "दूसरे ऐप में खोलें"
    override val eventsOnThisDay = "इस दिन के आयोजन"
    override val happeningOn = "आयोजन की तिथि"
    override val kindTodo = "काम"
    override val kindEvent = "आयोजन"
    override val addEventTitle = "आयोजन जोड़ें"
    override val addEventIntro = "किसी तारीख़ का समारोह या मिलन — यह कैलेंडर में दिखता है, कामों की सूची में नहीं।"
    override val addEventButton = "आयोजन जोड़ें"
    override val eventNote = "आयोजन — इस तारीख़ का अवसर, इसे पूरा नहीं करना"
    override val addEventHere = "+ आयोजन जोड़ें"
    override fun tipInvolves(party: String) = "इसमें $party शामिल"
    override fun tipComments(n: Int) = "$n टिप्पणी"
    override fun tipFiles(n: Int) = "$n संलग्नक"
    override val homeEyebrow = "जल्द ही विवाह"
    override val homeSetDates = "उलटी गिनती शुरू करने के लिए मुख्य तिथियाँ तय करें"
    override fun progressOf(done: Int, total: Int) = "$total में से $done काम पूरे"
    override val nextUp = "आगे"
    override val openChecklist = "चेकलिस्ट खोलें"
    override val tapToOpen = "दस्तावेज़ खोलने के लिए उस पर टैप करें।"
    override val remove = "हटाएँ"
    override val comments = "टिप्पणियाँ"
    override val earlierNote = "पुराना नोट"
    override val noComments = "अभी कोई टिप्पणी नहीं। बातचीत शुरू करें — दोनों परिवार इसे देख सकते हैं।"
    override val writeComment = "टिप्पणी लिखें… किसी को टैग करने के लिए @ लगाएँ"
    override val mentionSomeone = "किसी को टैग करें"
    override fun laneAdd(lane: String) = "$lane में काम जोड़ें"
    override val commentButton = "टिप्पणी करें"
    override val posting = "भेजा जा रहा है…"
    override val delete = "मिटाएँ"
    override val removeTask = "काम हटाएँ"
    override val removeTaskTitle = "यह काम हटाएँ?"
    override val removeTaskBody = "यह सभी के लिए हट जाएगा।"
    override val cancel = "रद्द करें"
    override fun overLimit(name: String) = "$name 8 MB से बड़ी है।"
    override fun noAppCanOpen(name: String) = "इस डिवाइस पर कोई ऐप $name नहीं खोल सकता।"
    override fun couldntOpen(name: String) = "$name नहीं खुल सकी।"
    override fun couldntUpload(name: String) = "$name अपलोड नहीं हो सकी।"
    override val couldntLoadComments = "टिप्पणियाँ लोड नहीं हो सकीं।"
    override val couldntPost = "टिप्पणी नहीं भेजी जा सकी।"
    override val couldntDeleteComment = "टिप्पणी नहीं मिटाई जा सकी।"
    override val couldntRemoveFile = "फ़ाइल नहीं हटाई जा सकी।"
    override val nothingDue = "इस दिन कुछ देय नहीं है"
    override fun tasksSummary(n: Int, done: Int) = "$n काम · $done पूर्ण"
    override val addOnThisDay = "+ इस दिन के लिए काम जोड़ें"
    override val newTaskTitle = "नया काम"
    override val whatNeedsDoing = "क्या करना है?"
    override val assignTo = "कौन-कौन शामिल"
    override val anyoneCanMark = "आप जिसे चुनेंगे वह इसे पूर्ण कर सकता है।"
    override val phase = "चरण"
    override val dueDate = "नियत तिथि"
    override val createTask = "काम बनाएँ"
    override val keyDatesIntro = "हर काम की नियत तिथि इन्हीं से तय होती है। एक बदलें तो पूरी योजना खिसक जाती है।"
    override val tradition = "परंपरा"
    override val keyDatesRule = "सगाई विवाह से पहले या उसी दिन होनी चाहिए, और हनीमून विवाह के बाद या उसी दिन।"
    override val save = "सहेजें"
    override val ok = "ठीक है"
    override val traditionNames = mapOf(
        "" to "सभी ब्राह्मण परंपराएँ", "tamil" to "तमिल (अय्यर / अय्यंगार)", "telugu" to "तेलुगु", "kannada" to "कन्नड़ (स्मार्त / माध्व)",
        "malayali" to "मलयाली (नंबूदिरी / अय्यर)", "north" to "उत्तर भारतीय", "bengali" to "बंगाली", "marathi" to "मराठी",
        "gujarati" to "गुजराती", "odia" to "ओड़िया",
    )
    override val filtersTitle = "फ़िल्टर"
    override val clearAll = "सब हटाएँ"
    override fun matchesCount(shown: Int, total: Int) = "$total में से $shown काम मेल खाते हैं"
    override val quickViews = "त्वरित दृश्य"
    override val facetAssignedTo = "कौन-कौन शामिल"
    override val facetType = "प्रकार"
    override val facetPhase = "चरण"
    override val facetStatus = "स्थिति"
    override val statusTodo = "करना है"
    override val statusDone = "पूर्ण"
    override val include = "शामिल करें"
    override val exclude = "बाहर रखें"
    override fun showTasks(n: Int) = "$n काम दिखाएँ"
}

object WeddingStringsTa : WeddingStrings {
    override val loading = "ஏற்றுகிறது…"
    override val notAvailableTitle = "இன்னும் கிடைக்கவில்லை"
    override val notAvailableBody = "உங்கள் பிள்ளையின் சுயவிவரம் இணைந்ததும் பட்டியல் திறக்கும்."
    override val noTasksTitle = "வேலைகள் இல்லை"
    override val noTasksBody = "வேறு வடிகட்டிகளை முயற்சிக்கவும்."
    override val clearFilters = "வடிகட்டிகளை நீக்கு"
    override val loadError = "திருமணப் பட்டியலை ஏற்ற முடியவில்லை."
    override val actionError = "அது நடக்கவில்லை. மீண்டும் முயற்சிக்கவும்."
    override val layoutList = "பட்டியல்"
    override val layoutBoard = "பலகை"
    override val layoutCalendar = "நாட்காட்டி"
    override val filtersButton = "வடிகட்டி"
    override fun filtersButtonCount(n: Int) = "வடிகட்டி ($n)"
    override val searchPlaceholder = "வேலைகளைத் தேடுக…"
    override fun groupBy(name: String) = "குழு: $name ▾"
    override val groupPhase = "கட்டம்"
    override val groupDue = "இறுதி தேதி"
    override val groupOwner = "நபர்"
    override val groupParty = "விற்பனையாளர் / நிபுணர்"
    override val noParty = "குடும்பங்கள் மட்டும்"
    override val newTaskButton = "+ புதிது"
    override val addInGroup = "+ சேர்"
    override fun moreNarrow(n: Int) = "+$n மேலும் — வடிகட்டிகளால் சுருக்கவும்"
    override fun percentDone(pct: Int) = "$pct% முடிந்தது"
    override fun daysToWedding(n: Int) = "திருமணத்திற்கு $n நாட்கள்"
    override fun daysSinceWedding(n: Int) = "திருமணமாகி $n நாட்கள்"
    override val setKeyDatesHint = "முக்கிய தேதிகளை அமைக்கவும்"
    override fun chipOverdue(n: Int) = "தாமதம் $n"
    override fun chipThisWeek(n: Int) = "இந்த வாரம் $n"
    override fun chipMine(n: Int) = "என்னுடையவை $n"
    override val keyDates = "முக்கிய தேதிகள்"
    override fun tasksOfTotal(shown: Int, total: Int) = "$total இல் $shown வேலைகள்"
    override val me = "நான்"
    override val bride = "மணமகள்"
    override val groom = "மணமகன்"
    override val brideParents = "மணமகளின் பெற்றோர்"
    override val groomParents = "மணமகனின் பெற்றோர்"
    override fun parentsOf(name: String) = "$name இன் பெற்றோர்"
    override val sharedDecisions = "பகிர்ந்த முடிவுகள்"
    override val scopeIndividual = "தனிப்பட்டது"
    override val scopeCouple = "மணமகள் & மணமகன்"
    override val scopeParents = "இருவரின் பெற்றோர்"
    override val scopeFamily = "குடும்பத்திற்குள்"
    override val scopeGroup = "நால்வரும்"
    override val scopeShared = "பகிர்ந்தது"
    override val bucketOverdue = "தாமதம்"
    override val bucketNext7 = "அடுத்த 7 நாட்கள்"
    override val bucketNext30 = "அடுத்த 30 நாட்கள்"
    override val bucketLater = "பின்னர்"
    override val bucketNoDate = "தேதி இல்லை"
    override val bucketDone = "முடிந்தது"
    override val quickMine = "என் திறந்த வேலைகள்"
    override val quickOverdue = "தாமதம்"
    override val quickWeek = "அடுத்த 7 நாட்களில் முடிக்க வேண்டியவை"
    override val quickJoint = "கூட்டு முடிவுகள்"
    override val visEveryone = "அனைவரும்"
    override val visMyFamily = "என் குடும்பம் மட்டும்"
    override val visOnlyMe = "நான் மட்டும்"
    override val relToday = "இன்று"
    override val relTomorrow = "நாளை"
    override val relYesterday = "நேற்று"
    override fun relDaysOverdue(n: Int) = "$n நாள் தாமதம்"
    override fun relInDays(n: Int) = "$n நாட்களில்"
    override val noDate = "தேதி இல்லை"
    override val agoJustNow = "இப்போது"
    override fun agoMinutes(n: Int) = "$n நிமிடம் முன்"
    override fun agoHours(n: Int) = "$n மணி முன்"
    override fun agoDays(n: Int) = "$n நாள் முன்"
    override val monthsShort = listOf("ஜன.", "பிப்.", "மார்.", "ஏப்.", "மே", "ஜூன்", "ஜூலை", "ஆக.", "செப்.", "அக்.", "நவ.", "டிச.")
    override val monthsLong = listOf("ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்", "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்")
    override val weekdaysShort = listOf("திங்", "செவ்", "புத", "வியா", "வெள்", "சனி", "ஞாயி")
    override val weekdaysLong = listOf("திங்கள்", "செவ்வாய்", "புதன்", "வியாழன்", "வெள்ளி", "சனி", "ஞாயிறு")
    override val calToday = "இன்று"
    override val calWedding = "திருமணம்"
    override fun dueThisMonth(n: Int) = "இந்த மாதம் $n வேலைகள்"
    override fun withoutDate(n: Int) = "$n தேதி இல்லாதவை"
    override val engagement = "நிச்சயதார்த்தம்"
    override val marriage = "திருமணம்"
    override val honeymoon = "தேனிலவு"
    override val engagementShort = "நிச்."
    override val weddingShort = "திரு."
    override val honeymoonShort = "தேனி."
    override val pmMuhoortham = "முகூர்த்தம்"
    override val pmPurnima = "பௌர்ணமி"
    override val pmAmavasya = "அமாவாசை"
    override val pmChandrashtamam = "சந்திராஷ்டமம்"
    override val pmShubh = "சுப முகூர்த்தம்"
    override val pmAmavasyaAvoid = "அமாவாசை — பொதுவாகத் தவிர்க்கப்படும்"
    override fun pmChandrashtamamFor(names: String) = "சந்திராஷ்டமம் — $names"
    override val pmChecking = "பஞ்சாங்கம் பார்க்கப்படுகிறது…"
    override val taskFallback = "வேலை"
    override val markDone = "முடிந்தது எனக் குறி"
    override val markAgreed = "ஒப்புதல் / முடிந்தது எனக் குறி"
    override val doneReopen = "✓ முடிந்தது — மீண்டும் திற"
    override fun assignedOnly(names: String) = "$names மட்டுமே இதை முடிந்ததாகக் குறிக்கலாம்"
    override fun signedOffBy(names: String) = "$names முடித்தார்"
    override fun withParty(party: String) = "$party உடன்"
    override val consult = "ஆலோசனை"
    override fun commentsPill(n: Int) = "$n கருத்து"
    override fun filesPill(n: Int) = "$n கோப்பு"
    override val assigned = "யார் யார்"
    override val everyone = "அனைவரும்"
    override val bothParents = "இருவரின் பெற்றோர்"
    override fun nameAndParents(name: String) = "$name & பெற்றோர்"
    override fun nPeople(n: Int) = "$n பேர்"
    override val addTodoTitle = "வேலையைச் சேர்"
    override val addTodoIntro = "உங்கள் குடும்பங்கள் செய்ய, முடிவு செய்ய அல்லது ஏற்பாடு செய்ய வேண்டிய ஒன்று."
    override val ownersHint = "இதை ஏற்கும் அனைவரையும் தேர்ந்தெடுக்கவும்"
    override val anyOwnerCanMark = "இதில் உள்ள யாரும் அனைவருக்காகவும் முடிந்ததாகக் குறிக்கலாம்."
    override val whenLabel = "எப்போது"
    override val chipToday = "இன்று"
    override val chipInWeek = "ஒரு வாரத்தில்"
    override val chipInMonth = "ஒரு மாதத்தில்"
    override val noDateChip = "தேதி இல்லை"
    override val visibilityEveryoneHint = "இரு குடும்பங்களும் பார்க்கலாம்."
    override val visibilitySideHint = "உங்கள் குடும்பம் மட்டும் பார்க்கலாம்."
    override val visibilityOnlyMeHint = "நீங்கள் மட்டும் பார்க்கலாம்."
    override val addTodoButton = "வேலையைச் சேர்"
    override val milestoneEngagement = "நிச்சயதார்த்த விழா"
    override val milestoneMarriage = "முகூர்த்த நாள் — முந்தைய, பிந்தைய நாட்களும் திருமணமாகக் கணக்கிடப்படும்"
    override val milestoneHoneymoon = "சேர்ந்து செலவிடும் நேரம்"
    override val engagementBeforeWedding = "நிச்சயதார்த்தம் திருமணத்திற்கு முன் அல்லது அதே நாளில் இருக்க வேண்டும்."
    override val honeymoonAfterWedding = "தேனிலவு திருமணத்திற்குப் பின் அல்லது அதே நாளில் இருக்க வேண்டும்."
    override fun daysBeforeWedding(n: Int) = "திருமணத்திற்கு $n நாட்கள் முன்"
    override fun daysAfterWedding(n: Int) = "திருமணத்திற்கு $n நாட்கள் பின்"
    override fun inDays(n: Int) = if (n == 0) "இன்று" else "$n நாட்களில்"
    override fun daysAgo(n: Int) = "$n நாட்கள் முன்"
    override val saveDates = "தேதிகளைச் சேமி"
    override val traditionHint = "பிற பிராமண மரபுகளுக்குரிய வேலைகளை மறைக்கும்."
    override val ordinaryDay = "சிறப்புக் குறிகள் இல்லை — ஒரு சாதாரண நாள்."
    override val panchangam = "பஞ்சாங்கம்"
    override val pmMuhoortham1 = "புதிய தொடக்கங்களுக்கு உகந்த நாள்."
    override val pmPurnima1 = "முழு நிலவு."
    override val pmAmavasya1 = "அமாவாசை — விழாக்களுக்குப் பொதுவாகத் தவிர்க்கப்படும்."
    override val pmChandra1 = "சந்திரன் அவர்களின் ஜன்ம ராசியிலிருந்து 8-ஆம் ராசியில் உள்ளது."
    override val todosOnThisDay = "இந்த நாளின் வேலைகள்"
    override val chooseFromList = "உங்கள் பட்டியலிலிருந்து தேர்ந்தெடு"
    override val searchYourTodos = "உங்கள் வேலைகளைத் தேடுக…"
    override fun nowDueOn(date: String) = "தற்போது $date அன்று"
    override val noDateYet = "இன்னும் தேதி இல்லை"
    override val moveToThisDay = "இந்த நாளுக்கு மாற்று"
    override val nothingMatches = "பொருந்துவது எதுவுமில்லை."
    override val closeAction = "மூடு"
    override fun doneOfTotal(done: Int, total: Int) = "$total இல் $done முடிந்தது"
    override val creatorSuffix = " · உருவாக்கியவர்"
    override val signedOff = "முடித்தார்"
    override val pending = "நிலுவையில்"
    override val due = "இறுதி தேதி"
    override val setDate = "தேதியைத் தேர்ந்தெடுக்கவும்"
    override val clear = "நீக்கு"
    override val resetDate = "கணக்கிட்ட தேதிக்கு மாற்று"
    override val saveTitle = "தலைப்பைச் சேமி"
    override val titlePlaceholder = "வேலையின் தலைப்பு"
    override val visibleTo = "யாருக்குத் தெரியும்"
    override val anchorCustom = "தனிப்பட்ட தேதி"
    override fun anchorOn(anchor: String) = "$anchor தேதியன்று"
    override fun anchorBefore(days: Int, anchor: String) = "$anchor தேதிக்கு $days நாட்கள் முன்"
    override fun anchorAfter(days: Int, anchor: String) = "$anchor தேதிக்கு $days நாட்கள் பின்"
    override val anchorEngagement = "நிச்சயதார்த்த"
    override val anchorMarriage = "திருமண"
    override val anchorHoneymoon = "தேனிலவு"
    override val setKeyDatesToSchedule = "இதை திட்டமிட முக்கிய தேதிகளை அமைக்கவும்"
    override val attachments = "இணைப்புகள்"
    override val addFile = "+ கோப்பைச் சேர்"
    override val uploading = "பதிவேற்றுகிறது…"
    override val attachmentsHint = "விலைப்புள்ளிகள், புகைப்படங்கள், PDF — இரு குடும்பங்களும் பார்க்கலாம்."
    override val opening = "திறக்கிறது…"
    override val openWith = "வேறு ஆப்பில் திறக்க"
    override val eventsOnThisDay = "இந்த நாளின் நிகழ்வுகள்"
    override val happeningOn = "நிகழ்வு நடக்கும் தேதி"
    override val kindTodo = "பணி"
    override val kindEvent = "நிகழ்வு"
    override val addEventTitle = "நிகழ்வைச் சேர்"
    override val addEventIntro = "ஒரு தேதியில் நடக்கும் விழா அல்லது கூடுகை — இது நாட்காட்டியில் தெரியும், பணிப் பட்டியலில் அல்ல."
    override val addEventButton = "நிகழ்வைச் சேர்"
    override val eventNote = "நிகழ்வு — இந்தத் தேதியின் நிகழ்ச்சி, முடித்துக் குறிக்க ஒன்றுமில்லை"
    override val addEventHere = "+ நிகழ்வைச் சேர்"
    override fun tipInvolves(party: String) = "$party தொடர்புடையவர்"
    override fun tipComments(n: Int) = "$n கருத்து"
    override fun tipFiles(n: Int) = "$n இணைப்பு"
    override val homeEyebrow = "விரைவில் திருமணம்"
    override val homeSetDates = "கவுண்டவுன் தொடங்க முக்கிய தேதிகளை அமைக்கவும்"
    override fun progressOf(done: Int, total: Int) = "$total இல் $done பணிகள் முடிந்தன"
    override val nextUp = "அடுத்து"
    override val openChecklist = "சரிபார்ப்புப் பட்டியலைத் திற"
    override val tapToOpen = "ஆவணத்தைத் திறக்க அதைத் தட்டவும்."
    override val remove = "நீக்கு"
    override val comments = "கருத்துகள்"
    override val earlierNote = "முந்தைய குறிப்பு"
    override val noComments = "இன்னும் கருத்துகள் இல்லை. உரையாடலைத் தொடங்குங்கள் — இரு குடும்பங்களும் பார்க்கலாம்."
    override val writeComment = "கருத்தை எழுதுக… ஒருவரைக் குறிப்பிட @ தட்டச்சிடுக"
    override val mentionSomeone = "ஒருவரைக் குறிப்பிடு"
    override fun laneAdd(lane: String) = "$lane இல் வேலையைச் சேர்"
    override val commentButton = "கருத்திடு"
    override val posting = "அனுப்புகிறது…"
    override val delete = "அழி"
    override val removeTask = "வேலையை நீக்கு"
    override val removeTaskTitle = "இந்த வேலையை நீக்கவா?"
    override val removeTaskBody = "இது அனைவருக்கும் மறைந்துவிடும்."
    override val cancel = "ரத்துசெய்"
    override fun overLimit(name: String) = "$name 8 MB ஐ விடப் பெரியது."
    override fun noAppCanOpen(name: String) = "இந்தச் சாதனத்தில் $name ஐத் திறக்கக்கூடிய செயலி இல்லை."
    override fun couldntOpen(name: String) = "$name ஐத் திறக்க முடியவில்லை."
    override fun couldntUpload(name: String) = "$name ஐப் பதிவேற்ற முடியவில்லை."
    override val couldntLoadComments = "கருத்துகளை ஏற்ற முடியவில்லை."
    override val couldntPost = "கருத்தை அனுப்ப முடியவில்லை."
    override val couldntDeleteComment = "கருத்தை அழிக்க முடியவில்லை."
    override val couldntRemoveFile = "கோப்பை நீக்க முடியவில்லை."
    override val nothingDue = "இந்த நாளுக்கு எதுவும் இல்லை"
    override fun tasksSummary(n: Int, done: Int) = "$n வேலைகள் · $done முடிந்தவை"
    override val addOnThisDay = "+ இந்த நாளுக்கு வேலையைச் சேர்"
    override val newTaskTitle = "புதிய வேலை"
    override val whatNeedsDoing = "என்ன செய்ய வேண்டும்?"
    override val assignTo = "யார் யார்"
    override val anyoneCanMark = "நீங்கள் தேர்ந்தெடுப்பவர் இதை முடிந்ததாகக் குறிக்கலாம்."
    override val phase = "கட்டம்"
    override val dueDate = "இறுதி தேதி"
    override val createTask = "வேலையை உருவாக்கு"
    override val keyDatesIntro = "ஒவ்வொரு வேலையின் இறுதி தேதியும் இவற்றிலிருந்தே கணக்கிடப்படுகிறது. ஒன்றை மாற்றினால் முழுத் திட்டமும் மாறும்."
    override val tradition = "பாரம்பரியம்"
    override val keyDatesRule = "நிச்சயதார்த்தம் திருமணத்திற்கு முன் அல்லது அதே நாளிலும், தேனிலவு திருமணத்திற்குப் பின் அல்லது அதே நாளிலும் இருக்க வேண்டும்."
    override val save = "சேமி"
    override val ok = "சரி"
    override val traditionNames = mapOf(
        "" to "அனைத்து பிராமண மரபுகள்", "tamil" to "தமிழ் (ஐயர் / ஐயங்கார்)", "telugu" to "தெலுங்கு", "kannada" to "கன்னடம் (ஸ்மார்த்த / மாத்வ)",
        "malayali" to "மலையாளி (நம்பூதிரி / ஐயர்)", "north" to "வட இந்தியர்", "bengali" to "வங்காளி", "marathi" to "மராத்தி",
        "gujarati" to "குஜராத்தி", "odia" to "ஒடியா",
    )
    override val filtersTitle = "வடிகட்டிகள்"
    override val clearAll = "அனைத்தையும் நீக்கு"
    override fun matchesCount(shown: Int, total: Int) = "$total இல் $shown வேலைகள் பொருந்துகின்றன"
    override val quickViews = "விரைவுக் காட்சிகள்"
    override val facetAssignedTo = "யார் யார்"
    override val facetType = "வகை"
    override val facetPhase = "கட்டம்"
    override val facetStatus = "நிலை"
    override val statusTodo = "செய்ய வேண்டியவை"
    override val statusDone = "முடிந்தவை"
    override val include = "சேர்"
    override val exclude = "விலக்கு"
    override fun showTasks(n: Int) = "$n வேலைகளைக் காட்டு"
}
