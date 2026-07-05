-- Create profiles table
CREATE TABLE public.profiles (
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

-- Create mentors table
CREATE TABLE public.mentors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    education TEXT,
    subjects TEXT,
    experience TEXT,
    image_url TEXT
);

-- Create courses table
CREATE TABLE public.courses (
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

-- Create enrollments table
CREATE TABLE public.enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES public.courses(id) ON DELETE CASCADE,
    price_paid TEXT,
    purchased_quarters TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    banned_until BIGINT,
    ban_reason TEXT
);

-- Create enrollment_requests table
CREATE TABLE public.enrollment_requests (
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

-- Create course_interactions table
CREATE TABLE public.course_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES public.courses(id) ON DELETE CASCADE,
    is_like BOOLEAN NOT NULL
);

