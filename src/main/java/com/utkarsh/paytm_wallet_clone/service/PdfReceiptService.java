package com.utkarsh.paytm_wallet_clone.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.utkarsh.paytm_wallet_clone.model.Transaction;
import com.utkarsh.paytm_wallet_clone.model.User;
import com.utkarsh.paytm_wallet_clone.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReceiptService {

    private final TransactionRepository transactionRepository;

    public PdfReceiptService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public byte[] generateReceipt(String txnRef, User user) {
        
        // Find transaction
        Transaction txn = transactionRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Verify user owns this transaction
        boolean belongsToUser = (txn.getSenderWallet() != null && 
                                  txn.getSenderWallet().getUser().getId().equals(user.getId())) ||
                                (txn.getReceiverWallet() != null && 
                                  txn.getReceiverWallet().getUser().getId().equals(user.getId()));

        if (!belongsToUser) {
            throw new RuntimeException("Unauthorized access to transaction");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Colors
            DeviceRgb primaryColor = new DeviceRgb(233, 30, 99); // Pink
            DeviceRgb lightGray = new DeviceRgb(240, 240, 240);

            // ─── Header ────────────────────────────────────────────────────────
            
            Paragraph header = new Paragraph("💳 PayFlow Wallet")
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(header);

            Paragraph subHeader = new Paragraph("Transaction Receipt")
                    .setFontSize(14)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subHeader);

            // ─── Transaction Details Table ─────────────────────────────────────

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            // Add rows
            addTableRow(table, "Transaction ID", txn.getTxnRef(), false);
            addTableRow(table, "Date & Time", 
                    txn.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), false);
            addTableRow(table, "Type", txn.getType().name(), false);
            addTableRow(table, "Status", txn.getStatus().name(), false);

            // Sender info
            if (txn.getSenderWallet() != null) {
                String senderName = txn.getSenderWallet().getUser().getName();
                String senderPhone = txn.getSenderWallet().getUser().getPhone();
                addTableRow(table, "From", senderName + " (" + senderPhone + ")", false);
            } else {
                addTableRow(table, "From", "External Deposit", false);
            }

            // Receiver info
            if (txn.getReceiverWallet() != null) {
                String receiverName = txn.getReceiverWallet().getUser().getName();
                String receiverPhone = txn.getReceiverWallet().getUser().getPhone();
                addTableRow(table, "To", receiverName + " (" + receiverPhone + ")", false);
            } else {
                addTableRow(table, "To", "External Withdrawal", false);
            }

            addTableRow(table, "Description", txn.getDescription(), false);
            
            // Amount row (highlighted)
            addTableRow(table, "Amount", "₹" + String.format("%.2f", txn.getAmount()), true);

            document.add(table);

            // ─── Footer ────────────────────────────────────────────────────────

            Paragraph footer = new Paragraph("This is a computer-generated receipt and does not require a signature.")
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30);
            document.add(footer);

            Paragraph contactInfo = new Paragraph("For support: support@payflow.com | +91-9999999999")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5);
            document.add(contactInfo);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF receipt", e);
        }
    }

    private void addTableRow(Table table, String label, String value, boolean highlight) {
        DeviceRgb primaryColor = new DeviceRgb(233, 30, 99);
        DeviceRgb lightGray = new DeviceRgb(240, 240, 240);

        Cell labelCell = new Cell()
                .add(new Paragraph(label).setBold())
                .setBackgroundColor(lightGray)
                .setBorder(new SolidBorder(ColorConstants.WHITE, 2))
                .setPadding(10);

        Cell valueCell = new Cell()
                .add(new Paragraph(value))
                .setBorder(new SolidBorder(ColorConstants.WHITE, 2))
                .setPadding(10);

        if (highlight) {
            labelCell.setBackgroundColor(primaryColor).setFontColor(ColorConstants.WHITE);
            valueCell.setBold().setFontSize(16);
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
