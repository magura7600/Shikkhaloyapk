import re

with open('supabase_schema.sql', 'r') as f:
    content = f.read()

target = """CREATE POLICY "Allow teachers to manage own courses" ON public.courses 
    FOR ALL TO authenticated
    USING (auth.uid() = channel_id AND (SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'teacher');"""

replacement = """CREATE POLICY "Allow teachers to manage own courses" ON public.courses 
    FOR ALL TO authenticated 
    USING (auth.uid() = channel_id AND (SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'teacher')
    WITH CHECK (auth.uid() = channel_id AND (SELECT role FROM public.profiles WHERE user_id = auth.uid()) = 'teacher');"""

content = content.replace(target, replacement)

with open('supabase_schema.sql', 'w') as f:
    f.write(content)
