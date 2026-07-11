import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Add the viewmodel import
if 'import com.example.viewmodel.CourseDetailViewModel' not in content:
    content = content.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport androidx.lifecycle.viewmodel.compose.viewModel\nimport com.example.viewmodel.CourseDetailViewModel')

# Update function signature
old_sig = """fun CourseDetailScreen(
    course: CourseItem,
    profile: UserProfile,
    mentors: List<Mentor>,
    userEnrollment: Enrollment?,
    isLiked: Boolean,
    courseInteractions: List<CourseInteraction> = emptyList(),
    pendingRequest: EnrollmentRequest? = null,
    onPurchaseClick: () -> Unit = {},
    onEnroll: (purchasedQuarters: String) -> Unit,
    onLikeToggle: () -> Unit,
    onCourseUpdate: (CourseItem) -> Unit,
    onMultipleCoursesUpdate: (List<CourseItem>) -> Unit = {},
    accentColor: Color,
    initialSubjectId: String? = null,
    initialChapterId: String? = null,
    initialClassId: String? = null,
    onClearInitialNavigation: () -> Unit = {},
    onBack: () -> Unit
)"""

new_sig = """fun CourseDetailScreen(
    initialCourse: CourseItem,
    profile: UserProfile,
    userEnrollment: Enrollment?,
    pendingRequest: EnrollmentRequest? = null,
    onPurchaseClick: () -> Unit = {},
    onEnroll: (purchasedQuarters: String) -> Unit,
    onMultipleCoursesUpdate: (List<CourseItem>) -> Unit = {},
    accentColor: Color,
    initialSubjectId: String? = null,
    initialChapterId: String? = null,
    initialClassId: String? = null,
    onClearInitialNavigation: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: CourseDetailViewModel = viewModel()
)"""

content = content.replace(old_sig, new_sig)

# Inside the function, we need to collect state
old_start = """    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }"""

new_start = """    val uiState by viewModel.uiState.collectAsState()
    val course = uiState.course ?: initialCourse
    val mentors = uiState.mentors
    val courseInteractions = uiState.interactions
    val isLiked = courseInteractions.any { it.user_id == profile.user_id && it.is_like }

    LaunchedEffect(initialCourse.id) {
        viewModel.loadCourseDetails(initialCourse)
    }

    var selectedQuarters by remember { mutableStateOf(setOf<CourseQuarter>()) }"""

content = content.replace(old_start, new_start)

# Replace onLikeToggle with viewModel.toggleLike(profile.user_id)
content = content.replace('onLikeToggle()', 'viewModel.toggleLike(profile.user_id)')
content = content.replace('onLikeToggle = onLikeToggle,', '')
content = content.replace('onLikeToggle = { onLikeToggle() }', '')

# Replace onCourseUpdate(updatedCourse) with viewModel.updateCourse(updatedCourse)
content = content.replace('onCourseUpdate(updatedCourse)', 'viewModel.updateCourse(updatedCourse)')
content = content.replace('onCourseUpdate(it)', 'viewModel.updateCourse(it)')
# The edit subjects might also call onCourseUpdate
content = content.replace('onCourseUpdate = {', 'viewModel.updateCourse(') # wait this might be risky, let's just do direct replacements
content = re.sub(r'onCourseUpdate\s*\(\s*([^)]+)\s*\)', r'viewModel.updateCourse(\1)', content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
