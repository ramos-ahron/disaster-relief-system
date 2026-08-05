package edu.ucalgary.oop.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Someone who calls in looking for a person. Callers are matched by first name and phone number. */
public final class Inquirer extends Person {
    private static final Pattern PHONE = Pattern.compile("\\d{3}-\\d{3}-\\d{4}");

    private final String phoneNumber;
    private final List<Inquiry> inquiries = new ArrayList<>();

    public Inquirer(String firstName, String lastName, String phoneNumber) {
        super(firstName, lastName);
        if (phoneNumber == null || !PHONE.matcher(phoneNumber.trim()).matches()) {
            throw new IllegalArgumentException("Phone number must look like 000-000-0000");
        }
        this.phoneNumber = phoneNumber.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public List<Inquiry> getInquiries() {
        return Collections.unmodifiableList(inquiries);
    }

    /** @return false if the same inquiry (date and details) was already logged */
    public boolean log(Inquiry inquiry) {
        if (inquiries.contains(inquiry)) {
            return false;
        }
        return inquiries.add(inquiry);
    }

    public boolean isSameCaller(String firstName, String phoneNumber) {
        return getFirstName().equalsIgnoreCase(firstName.trim()) && this.phoneNumber.equals(phoneNumber.trim());
    }
}
