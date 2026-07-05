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
