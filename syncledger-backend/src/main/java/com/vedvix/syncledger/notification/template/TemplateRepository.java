package com.vedvix.syncledger.notification.template;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class TemplateRepository {

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadDefaultTemplates();
        log.info("Loaded {} notification templates", templates.size());
    }

    public String getTemplate(String name) {
        return templates.get(name);
    }

    public void saveTemplate(String name, String content) {
        templates.put(name, content);
        log.info("Saved template: {}", name);
    }

    public void deleteTemplate(String name) {
        templates.remove(name);
        log.info("Deleted template: {}", name);
    }

    public Map<String, String> getAllTemplates() {
        return Map.copyOf(templates);
    }

    private void loadDefaultTemplates() {
        // Welcome Email Template
        templates.put("welcome_email", """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f4f4f4; }
                    .button { background-color: #4CAF50; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 5px; display: inline-block; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to {{companyName}}!</h1>
                    </div>
                    <div class="content">
                        <p>Hi {{firstName}} {{lastName}},</p>
                        <p>Thank you for joining us. We're excited to have you on board!</p>
                        <p>Please click the button below to activate your account:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="{{activationLink}}" class="button">Activate Account</a>
                        </p>
                        <p>If the button doesn't work, copy and paste this link into your browser:</p>
                        <p style="word-break: break-all;">{{activationLink}}</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 {{companyName}}. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """);

        // OTP Email Template
        templates.put("otp_email", """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .otp-box { background-color: #f4f4f4; padding: 20px; text-align: center; 
                               font-size: 32px; font-weight: bold; letter-spacing: 5px; 
                               border-radius: 5px; margin: 20px 0; }
                    .warning { color: #ff9800; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Verification Code</h2>
                    <p>Hi {{firstName}},</p>
                    <p>Your verification code is:</p>
                    <div class="otp-box">{{otp}}</div>
                    <p class="warning">⚠️ This code will expire in {{expiryMinutes}} minutes.</p>
                    <p>If you didn't request this code, please ignore this email.</p>
                </div>
            </body>
            </html>
            """);

        // Password Reset Template
        templates.put("password_reset", """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { background-color: #2196F3; color: white; padding: 12px 24px; 
                              text-decoration: none; border-radius: 5px; display: inline-block; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Password Reset Request</h2>
                    <p>Hi {{firstName}},</p>
                    <p>We received a request to reset your password. Click the button below to proceed:</p>
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="{{resetLink}}" class="button">Reset Password</a>
                    </p>
                    <p>This link will expire in 1 hour.</p>
                    <p>If you didn't request a password reset, please ignore this email.</p>
                </div>
            </body>
            </html>
            """);

        // Order Confirmation Template
        templates.put("order_confirmation", """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .order-details { background-color: #f4f4f4; padding: 15px; border-radius: 5px; }
                    .total { font-size: 20px; font-weight: bold; color: #4CAF50; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Order Confirmation</h2>
                    <p>Hi {{firstName}},</p>
                    <p>Thank you for your order! Your order has been confirmed.</p>
                    <div class="order-details">
                        <p><strong>Order ID:</strong> {{orderId}}</p>
                        <p><strong>Order Date:</strong> {{orderDate}}</p>
                        <p class="total">Total: {{currency}}{{totalAmount}}</p>
                    </div>
                    <p>We'll send you another email when your order ships.</p>
                </div>
            </body>
            </html>
            """);

        // SMS OTP Template
        templates.put("sms_otp", "Your verification code is {{otp}}. Valid for {{expiryMinutes}} minutes. - {{companyName}}");

        // SMS Order Update Template
        templates.put("sms_order_update", "Hi {{firstName}}, your order {{orderId}} has been {{status}}. Track at: {{trackingLink}} - {{companyName}}");

        // ===================== Subscription Lifecycle Templates =====================

        templates.put("subscription_trial_welcome", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #1a73e8, #0d47a1); padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0; font-size: 24px;">Welcome to {{appName}}!</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 16px; color: #333;">Hi {{adminName}},</p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Thank you for signing up <strong>{{orgName}}</strong> on {{appName}}. Your <strong>{{trialDays}}-day free trial</strong> has started!
                  </p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">During your trial, you get full access to all features:</p>
                  <ul style="color: #555; line-height: 2;">
                    <li>Automated invoice processing from email</li>
                    <li>AI-powered data extraction</li>
                    <li>Multi-user approval workflows</li>
                    <li>Sage integration</li>
                    <li>Real-time dashboard analytics</li>
                  </ul>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/microsoft-config" style="background-color: #1a73e8; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Get Started</a>
                  </div>
                  <p style="font-size: 14px; color: #888; margin-top: 20px;">
                    <strong>Next step:</strong> Configure your Microsoft email integration to start processing invoices automatically.
                  </p>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_trial_expiring", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background-color: {{urgencyColor}}; padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Trial Expires in {{daysLeft}} Day{{daysSuffix}}</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Your free trial for <strong>{{orgName}}</strong> on {{appName}} will expire in <strong>{{daysLeft}} day{{daysSuffix}}</strong>.
                  </p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    To continue using all features without interruption, upgrade to a paid plan before your trial ends.
                  </p>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/subscription" style="background-color: #1a73e8; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Upgrade Now</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_trial_expired", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background-color: #dc3545; padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Your Trial Has Expired</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    The free trial for <strong>{{orgName}}</strong> on {{appName}} has expired. Your account access has been suspended.
                  </p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Your data is safe and will be retained for 30 days. Upgrade to a paid plan to restore access immediately.
                  </p>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/subscription" style="background-color: #28a745; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Reactivate Account</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_activated", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #28a745, #20c997); padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Subscription Activated!</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Great news! The <strong>{{planName}}</strong> plan for <strong>{{orgName}}</strong> is now active.
                  </p>
                  <div style="background-color: #f8f9fa; border-radius: 6px; padding: 20px; margin: 20px 0;">
                    <p style="margin: 5px 0; color: #555;"><strong>Plan:</strong> {{planName}}</p>
                    <p style="margin: 5px 0; color: #555;"><strong>Valid until:</strong> {{expiresAt}}</p>
                  </div>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/dashboard" style="background-color: #1a73e8; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Go to Dashboard</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_expiring", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background-color: #ff9800; padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Subscription Renews in {{daysLeft}} Days</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Your subscription for <strong>{{orgName}}</strong> on {{appName}} will renew in <strong>{{daysLeft}} days</strong>.
                    Please ensure your payment method is up to date.
                  </p>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/subscription" style="background-color: #1a73e8; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Manage Subscription</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_expired", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background-color: #dc3545; padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Subscription Expired</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    The subscription for <strong>{{orgName}}</strong> on {{appName}} has expired. Your account access has been suspended.
                  </p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Your data is safe. Renew your subscription to restore access immediately.
                  </p>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/subscription" style="background-color: #28a745; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Renew Now</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_cancelled", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background-color: #6c757d; padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Subscription Cancelled</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Your subscription for <strong>{{orgName}}</strong> has been cancelled.
                  </p>
                  <p style="font-size: 14px; color: #888;"><strong>Reason:</strong> {{reason}}</p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Your data will be retained for 30 days. You can reactivate at any time.
                  </p>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/subscription" style="background-color: #1a73e8; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Reactivate</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("subscription_payment_failed", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background-color: #dc3545; padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">Payment Failed</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    We were unable to process payment for <strong>{{orgName}}</strong>'s {{planName}} plan.
                  </p>
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    Please update your payment method to avoid service interruption. We will retry the charge automatically.
                  </p>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/subscription" style="background-color: #dc3545; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">Update Payment Method</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} &mdash; Automated Invoice Processing</p>
                </div>
              </div>
            </body>
            </html>
            """);

        templates.put("new_org_signup_admin", """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #6f42c1, #5a2d82); padding: 30px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0;">New Organization Signed Up</h1>
                </div>
                <div style="padding: 30px;">
                  <p style="font-size: 15px; color: #555; line-height: 1.6;">
                    A new organization has signed up for {{appName}}:
                  </p>
                  <div style="background-color: #f8f9fa; border-radius: 6px; padding: 20px; margin: 20px 0;">
                    <p style="margin: 5px 0; color: #555;"><strong>Organization:</strong> {{orgName}}</p>
                    <p style="margin: 5px 0; color: #555;"><strong>Email:</strong> {{orgEmail}}</p>
                    <p style="margin: 5px 0; color: #555;"><strong>Plan:</strong> 15-day Trial</p>
                  </div>
                  <div style="text-align: center; margin: 30px 0;">
                    <a href="{{baseUrl}}/super-admin" style="background-color: #6f42c1; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">View in Admin Panel</a>
                  </div>
                </div>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                  <p style="font-size: 12px; color: #888; margin: 0;">{{appName}} Admin Notification</p>
                </div>
              </div>
            </body>
            </html>
            """);
    }
}