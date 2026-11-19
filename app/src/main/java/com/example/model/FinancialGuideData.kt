package com.example.model

data class FinancialTip(
  val id: String,
  val titleFa: String,
  val titleEn: String,
  val summaryFa: String,
  val contentFa: List<String>,
  val category: String // "CREDIT", "BANKING", "LOANS"
)

object FinancialGuideData {
  val tips = listOf(
    FinancialTip(
      id = "fin_1",
      titleFa = "الفبای کردیت اسکور (Credit Score) در آمریکا و کانادا",
      titleEn = "Credit Score Basics",
      summaryFa = "چرا داشتن نمره اعتباری خوب برای زندگی در آمریکای شمالی حیاتی است؟",
      category = "CREDIT",
      contentFa = listOf(
        "کردیت اسکور عددی بین ۳۰۰ تا ۸۵۰ است که نشان‌دهنده خوش‌حسابی و اعتبار مالی شماست.",
        "بانک‌ها، صاحب‌خانه‌ها و حتی شرکت‌های بیمه قبل از دادن خانه یا خدمات به شما، این نمره را چک می‌کنند.",
        "نمره بالای ۷۰۰ به معنی اعتبار عالی و دسترسی به وام‌ها با سود پایین است."
      )
    ),
    FinancialTip(
      id = "fin_2",
      titleFa = "راهنمای گام‌به‌گام ترمیم کردیت خراب (Credit Repair)",
      titleEn = "How to Repair Damaged Credit",
      summaryFa = "چگونه نمره اعتباری آسیب‌دیده یا کم را دوباره بالا بکشیم؟",
      category = "CREDIT",
      contentFa = listOf(
        "۱. پرداخت به‌موقع اقساط: تاریخ سررسید قسط‌ها و کارت‌ها را روی حالت Auto-Pay بگذارید تا حتی یک روز هم دیر نشود.",
        "۲. کاهش نسبت مصرف (Credit Utilization): هیچ‌وقت بیشتر از ۳۰ درصد از سقف کارت اعتباری خود را پر نکنید (بهتر است زیر ۱۰ درصد باشد).",
        "۳. استفاده از Secured Credit Card: اگر کردیت شما صفر یا خراب است، با گذاشتن سپرده نقدی، کارت Secured بگیرید و با خوش‌حسابی آن را ترمیم کنید.",
        "۴. بررسی گزارش اعتباری (Credit Report): سالی یک‌بار گزارش خود را از Equifax یا TransUnion چک کنید و اگر خطایی دیدید، درخواست اصلاح (Dispute) بدهید."
      )
    ),
    FinancialTip(
      id = "fin_3",
      titleFa = "قوانین طلایی مدیریت حساب‌های بانکی و چک",
      titleEn = "Smart Banking & Checking Rules",
      summaryFa = "جلوگیری از کارمزدهای سنگین بانکی و برگشت خوردن چک (NSF)",
      category = "BANKING",
      contentFa = listOf(
        "موجودی حساب خود را همیشه رصد کنید تا به صفر یا منفی نرسد.",
        "بسیاری از بانک‌ها برای حفظ موجودی مینیمم (Minimum Balance)، کارمزد ماهانه می‌گیرند؛ بانکی را انتخاب کنید که حساب رایگان (Free Checking) داشته باشد.",
        "از چک شخصی (Personal Check) با احتیاط استفاده کنید و مطمئن شوید پول به اندازه کافی در حساب هست."
      )
    )
  )

  const val LEGAL_DISCLAIMER = "سلب مسئولیت قانونی: این بخش صرفاً جهت آگاهی‌رسانی و آموزش عمومی امور مالی در ایالات متحده و کانادا تهیه شده است و مشاوره مالی، حقوقی یا رسمی محسوب نمی‌شود. برای تصمیم‌گیری‌های مالی بزرگ حتماً با مشاور مالی دارای مجوز مشورت کنید."
}
