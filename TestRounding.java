public class TestRounding {
    public static void main(String[] args) {
        double rawSubtotal = 1.0 * 100.5;
        double subtotal = Math.round(rawSubtotal * 100.0) / 100.0;
        double rawTaxAmount = (rawSubtotal * 10.0) / 100.0;
        double taxAmount = Math.round(rawTaxAmount * 100.0) / 100.0;
        double cgstRaw = taxAmount / 2.0;
        double cgstRounded = Math.round(cgstRaw * 100.0) / 100.0;
        double sgstRounded = taxAmount - cgstRounded;
        System.out.println("subtotal " + subtotal + " taxAmount " + taxAmount + " cgstRounded " + cgstRounded + " sgstRounded " + sgstRounded + " totalAmount " + (subtotal + taxAmount));
    }
}
