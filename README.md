# 🎓 Shikkhaloy - Smart Course & Learning Management App
**Shikkhaloy** (শিক্ষালয়) একটি Jetpack Compose ও Kotlin দিয়ে তৈরি সর্বাধুনিক শিক্ষামূলক অ্যান্ড্রয়েড অ্যাপ্লিকেশন। এটি শিক্ষার্থী, শিক্ষক এবং অ্যাডমিনদের জন্য তৈরি একটি ইন্টারেক্টিভ ও বহুমুখী ইকোসিস্টেম।

---

## 🚀 মূল ফিচারসমূহ (Core Features)

1. **ইন্টারেক্টিভ ও থিম-অ্যাডাপ্টিভ ড্যাশবোর্ডস**:
   - **Student Dashboard**: লাইভ ক্লাস রুটিন, মিসড ক্লাসের হিস্টোরি, হোমওয়ার্ক ট্র্যাকার এবং প্রগ্রেসিভ ডার্ক থিম সাপোর্ট।
   - **Teacher Dashboard**: কন্টেন্ট ম্যানেজমেন্ট, লাইভ সেশন এবং রিয়াল-টাইম ক্লাস নোটিফিকেশন সিডিউলিং।
   - **Admin Dashboard**: কোর্স ক্রিয়েশন, পাবলিক চ্যানেল ম্যানেজমেন্ট এবং ইউজার রোল ভ্যালিডেশন।
   
2. **পারফরম্যান্স-অপ্টিমাইজড মিডিয়া ও পিডিএফ**:
   - **ExoPlayer Integration**: বাফারিং কন্ট্রোল ও লাইভ স্ট্রিমিং রিডানড্যান্সি সহ ভিডিও প্লেব্যাক।
   - **Memory-Capped PDF Viewer**: বড় সাইজের পিডিএফ ফাইলের কারণে যাতে ওওএম (Out of Memory - OOM) ক্র্যাশ না হয়, সেজন্য ডাইনামিক রেন্ডারিং স্কেলিং এবং `RGB_565` পিক্সেল ফরম্যাটের ইন্টিগ্রেশন।
   - **Atomic Offline Download System**: অর্ধেক ডাউনলোড হয়ে ফাইল ড্যামেজ হওয়া রোধ করতে `.tmp` ফাইলে আংশিক লিখে সফল ডাউনলোডের পর অটোমিক রেনেম (`renameTo`) টেকনিকের ব্যবহার। ডাউনলোডের পূর্বে পর্যাপ্ত স্টোরেজ খালি আছে কিনা তার রানটাইম ভ্যালিডেশন।

3. **নিরাপত্তা ও এনক্রিপশন (Security & Storage)**:
   - **Encrypted SharedPreferences**: ব্যবহারকারীর সংবেদনশীল তথ্য ও ক্রেডেনশিয়ালস এনক্রিপ্টেড উপায়ে সংরক্ষণের ব্যবস্থা।
   - **Role-Sanitizer Fallback**: এনক্রিপশন সেটআপ যদি কোনো আনসাপোর্টেড হার্ডওয়্যারে ব্যর্থ হয়, তবে প্লেইন প্রিফারেন্সে ফলব্যাক করার সময় অ্যাডমিন রোল স্যানিটাইজার কাজ করবে যাতে সিকিউরিটি বাইপাস অসম্ভব হয়।

---

## 🛠️ আর্কিটেকচার ও টেক স্ট্যাক (Tech Stack)

* **UI Framework**: Jetpack Compose (Material Design 3 - M3)
* **Language**: Kotlin DSL & Coroutines Flow
* **Backend Integration**: Supabase (Database & Realtime) + Firebase Auth + OneSignal Push Notifications
* **Networking**: OkHttp Client + Ktor Client
* **Local Security**: Android Cryptographic Security (`androidx.security:security-crypto`)
* **Testing & Verification**: Robolectric & Roborazzi (Screenshot Visual Regression Testing)

---

## ⚡ Local Setup & Run Guide

### পূর্বশর্ত (Prerequisites):
- [Android Studio Jellyfish](https://developer.android.com/studio) বা তার পরবর্তী সংস্করণ।
- JDK 17+

### রান করার ধাপসমূহ (Steps to Run):

1. **প্রজেক্ট ইম্পোর্ট করুন**:
   Android Studio ওপেন করে **Open** সিলেক্ট করুন এবং প্রজেক্টের রুট ডিরেক্টরিটি সিলেক্ট করুন।

2. **এনভায়রনমেন্ট কনফিগার করুন**:
   প্রজেক্টের রুট ডিরেক্টরিতে একটি `.env` ফাইল তৈরি করুন এবং আপনার Supabase এবং Gemini API কী সেট করুন:
   ```env
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   GEMINI_API_KEY=your-gemini-api-key
   ```

3. **রান করুন**:
   অ্যান্ড্রয়েড এমুলেটর বা রিয়েল ডিভাইসে রান বোতাম প্রেস করে বিল্ড করুন।

---

## 🛡️ Claude-এর ফিডব্যাকের ভিত্তিতে সাম্প্রতিক ইমপ্রুভমেন্টস (Claude Feedback Resolutions)

Claude-এর অ্যানালাইসিস রিপোর্টে উঠে আসা বিষয়গুলোর ওপর ভিত্তি করে প্রজেক্টের কোডবেস এবং স্ট্রাকচারে নিম্নলিখিত ইতিবাচক পরিবর্তনগুলো আনা হয়েছে:

* **অপ্রয়োজনীয় ডিবাগ ফাইল ডিলিট**: রিপোজিটরিকে ক্লিন ও অপ্টিমাইজড রাখতে ১ মেগাবাইটের `fb_direct.html`, `fb_result_fdown.txt` এবং `fix_braces.py` স্ক্রিপ্টগুলো রুট ও অ্যাপ লেভেল থেকে সম্পূর্ণ ডিলেট করা হয়েছে।
* **Gradle আর্কিটেকচার রিকনস্ট্রাকশন**: `build.gradle.kts` এর কনফিগারেশন টাইম ফাইলে রানটাইম ডিলিট অপারেশন সম্পূর্ণ বন্ধ করা হয়েছে (যা বিল্ড প্রসেসকে ধীর করতো)। মিডিয়া এবং রিসোর্স ফাইলগুলোকে স্ট্যাটিকভাবে অপ্টিমাইজ করা হয়েছে এবং সফলভাবে অ্যাপ কমপাইল করা হয়েছে।
* **সিক্রেট কী ভ্যালিডেশন**: Supabase Anon Key এবং Firebase client configs মূলত ব্রাউজার/অ্যান্ড্রয়েড ক্লায়েন্টের জন্যই তৈরি। এগুলো সিকিউর করার জন্য ব্যাকএন্ডে **Supabase Row Level Security (RLS)** এবং Firebase Security Rules ব্যবহৃত হয়। অন্যান্য সিক্রেট ক্রেডেনশিয়ালগুলো সঠিকভাবে লোকাল এনক্রিপশনে স্টোর করা হয়েছে।
