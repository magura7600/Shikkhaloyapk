-- 1. Create profiles table
CREATE TABLE IF NOT EXISTS public.profiles (
    user_id UUID PRIMARY KEY,
    email TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'student',
    full_name TEXT NOT NULL,
    institution TEXT,
    contact TEXT,
    uid_code TEXT NOT NULL,
    profile_image_url TEXT,
    handle TEXT,
    description TEXT,
    cover_image_url TEXT,
    is_banned BOOLEAN DEFAULT false
);

-- 2. Create mentors table
CREATE TABLE IF NOT EXISTS public.mentors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    education TEXT,
    subjects TEXT,
    experience TEXT,
    image_url TEXT
);

-- 3. Create courses table
CREATE TABLE IF NOT EXISTS public.courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    pricingOption TEXT DEFAULT 'Fully Paid',
    mainPrice TEXT,
    discountPrice TEXT,
    bkashNumber TEXT,
    nagadNumber TEXT,
    rocketNumber TEXT,
    paymentDetails TEXT,
    isQuarterOn BOOLEAN DEFAULT false,
    quarters JSONB DEFAULT '[]'::jsonb,
    subjects JSONB DEFAULT '[]'::jsonb
);

-- 4. Create enrollments table
CREATE TABLE IF NOT EXISTS public.enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES public.courses(id) ON DELETE CASCADE,
    price_paid TEXT,
    purchased_quarters TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    banned_until BIGINT,
    ban_reason TEXT
);

-- 5. Create enrollment_requests table
CREATE TABLE IF NOT EXISTS public.enrollment_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES public.courses(id) ON DELETE CASCADE,
    requested_quarters TEXT,
    amount TEXT,
    payment_method TEXT,
    sender_number TEXT,
    transaction_id TEXT,
    status TEXT DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 6. Create course_interactions table
CREATE TABLE IF NOT EXISTS public.course_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES public.courses(id) ON DELETE CASCADE,
    is_like BOOLEAN NOT NULL
);

-- 7. Create app_updates table
CREATE TABLE IF NOT EXISTS public.app_updates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version_code BIGINT NOT NULL,
    version_name TEXT NOT NULL,
    apk_url TEXT NOT NULL,
    changelog TEXT DEFAULT '',
    is_force_update BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 8. Create app_notices table
CREATE TABLE IF NOT EXISTS public.app_notices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    image_url TEXT,
    type TEXT DEFAULT 'general',
    action_url TEXT,
    scheduled_time TEXT,
    target_course_id TEXT
);

-- ==========================================
-- ROW LEVEL SECURITY (RLS) & ACCESS POLICIES
-- ==========================================

-- 1. Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mentors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.enrollments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.enrollment_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.course_interactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_updates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_notices ENABLE ROW LEVEL SECURITY;

-- 2. Define Policies for "profiles"
CREATE POLICY "Allow read profiles for everyone" ON public.profiles 
    FOR SELECT USING (true);

CREATE POLICY "Allow users to insert their own profile as student" ON public.profiles 
    FOR INSERT WITH CHECK (auth.uid() = user_id AND role = 'student');

CREATE POLICY "Allow users to update their own non-sensitive profile fields" ON public.profiles 
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id AND role = (SELECT role FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY "Allow admins full access to profiles" ON public.profiles 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

-- 3. Define Policies for "mentors"
CREATE POLICY "Allow public read mentors" ON public.mentors 
    FOR SELECT USING (true);

CREATE POLICY "Allow admin full access to mentors" ON public.mentors 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

-- 4. Define Policies for "courses"
CREATE POLICY "Allow public read courses" ON public.courses 
    FOR SELECT USING (true);

CREATE POLICY "Allow admin full access to courses" ON public.courses 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

-- 5. Define Policies for "enrollments"
CREATE POLICY "Allow users to read own enrollments" ON public.enrollments 
    FOR SELECT TO authenticated USING (auth.uid() = user_id OR (SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

CREATE POLICY "Allow admin full access to enrollments" ON public.enrollments 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

-- 6. Define Policies for "enrollment_requests"
CREATE POLICY "Allow users to read own enrollment requests" ON public.enrollment_requests 
    FOR SELECT TO authenticated USING (auth.uid() = user_id OR (SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

CREATE POLICY "Allow users to insert own enrollment requests" ON public.enrollment_requests 
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Allow admin full access to enrollment requests" ON public.enrollment_requests 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

-- 7. Define Policies for "course_interactions"
CREATE POLICY "Allow public read interactions" ON public.course_interactions 
    FOR SELECT USING (true);

CREATE POLICY "Allow users to manage own interactions" ON public.course_interactions 
    FOR ALL TO authenticated USING (auth.uid() = user_id);

-- 8. Define Policies for "app_updates"
CREATE POLICY "Allow public read app updates" ON public.app_updates 
    FOR SELECT USING (true);

CREATE POLICY "Allow admin full access to app updates" ON public.app_updates 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');

-- 9. Define Policies for "app_notices"
CREATE POLICY "Allow public read app notices" ON public.app_notices 
    FOR SELECT USING (true);

CREATE POLICY "Allow admin full access to app notices" ON public.app_notices 
    FOR ALL TO authenticated USING ((SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'admin');
