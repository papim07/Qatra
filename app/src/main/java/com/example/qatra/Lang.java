package com.example.qatra;

import java.util.HashMap;

public class Lang {
    private static boolean arabic = false;
    private static final HashMap<String, String> AR = new HashMap<>();

    static {
        AR.put("Qatra", "قطرة");
        AR.put("Right donor, right time", "المتبرع المناسب في الوقت المناسب");
        AR.put("Username", "اسم المستخدم");
        AR.put("Password", "كلمة المرور");
        AR.put("Log in", "تسجيل الدخول");
        AR.put("Create donor account", "إنشاء حساب متبرع");
        AR.put("Register", "تسجيل");
        AR.put("Full name", "الاسم الكامل");
        AR.put("Phone", "الهاتف");
        AR.put("Birth year", "سنة الميلاد");
        AR.put("Blood type", "فصيلة الدم");
        AR.put("City", "المدينة");
        AR.put("Notify me by", "أرسل التنبيهات عبر");
        AR.put("I agree", "أوافق");
        AR.put("Back", "رجوع");
        AR.put("Log out", "خروج");
        AR.put("My eligibility", "أهليتي");
        AR.put("You can donate now", "يمكنك التبرع الآن");
        AR.put("Donation requests", "طلبات التبرع");
        AR.put("Book appointment", "احجز موعدًا");
        AR.put("Confirm booking", "تأكيد الحجز");
        AR.put("My donations", "تبرعاتي");
        AR.put("Edit profile", "تعديل الملف");
        AR.put("Save", "حفظ");
        AR.put("Staff Dashboard", "لوحة الموظفين");
        AR.put("Stock & 14-day forecast", "المخزون وتوقع ١٤ يومًا");
        AR.put("Approve & send", "موافقة وإرسال");
        AR.put("Reject", "رفض");
        AR.put("Update stock", "تحديث المخزون");
        AR.put("Appeals", "النداءات");
        AR.put("Today's visits", "زيارات اليوم");
        AR.put("Administrator", "المسؤول");
        AR.put("Eligibility rules", "قواعد الأهلية");
        AR.put("Users", "المستخدمون");
        AR.put("Supply report", "تقرير الإمدادات");
        AR.put("Audit log", "سجل التدقيق");
        AR.put("Health questions", "أسئلة صحية");
        AR.put("Submit", "إرسال");
        AR.put("Details", "التفاصيل");
        AR.put("Response", "الاستجابة");
        AR.put("Booked", "محجوز");
        AR.put("Low", "منخفض");
        AR.put("Medium", "متوسط");
        AR.put("High", "مرتفع");
        AR.put("No requests right now. We only contact you when your blood type is needed.",
                "لا توجد طلبات الآن. نتواصل معك فقط عند الحاجة لفصيلتك.");
    }

    public static String t(String english) {
        if (!arabic) return english;
        String ar = AR.get(english);
        return ar == null ? english : ar;
    }

    public static void toggle() { arabic = !arabic; }
    public static boolean isArabic() { return arabic; }
}
