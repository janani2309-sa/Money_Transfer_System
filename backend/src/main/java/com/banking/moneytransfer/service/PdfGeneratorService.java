package com.banking.moneytransfer.service;

import com.banking.moneytransfer.domain.Account;
import com.banking.moneytransfer.dto.TransactionLogResponse;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.awt.Color;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ByteArrayInputStream generateStatementPdf(Account account, List<TransactionLogResponse> transactions) {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(15, 23, 42));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 41, 59));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(71, 85, 105));
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(15, 23, 42));
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(15, 23, 42));

            // 1. Header (Bank Info)
            Paragraph bankName = new Paragraph("EMPOWER BANKING", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(59, 130, 246)));
            bankName.setAlignment(Element.ALIGN_LEFT);
            document.add(bankName);

            Paragraph bankAddress = new Paragraph("123 Financial District, Tech Park, Suite 400\nsupport@empowerbanking.com", subtitleFont);
            bankAddress.setAlignment(Element.ALIGN_LEFT);
            bankAddress.setSpacingAfter(15);
            document.add(bankAddress);

            // Line separator
            Paragraph line = new Paragraph("----------------------------------------------------------------------------------------------------------------------------------");
            line.setFont(FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(226, 232, 240)));
            line.setSpacingAfter(15);
            document.add(line);

            // Title
            Paragraph docTitle = new Paragraph("Account Transaction Statement", titleFont);
            docTitle.setAlignment(Element.ALIGN_CENTER);
            docTitle.setSpacingAfter(25);
            document.add(docTitle);

            // 2. Account Details Section
            Paragraph accountSectionTitle = new Paragraph("Account Summary", sectionTitleFont);
            accountSectionTitle.setSpacingAfter(10);
            document.add(accountSectionTitle);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(25);
            summaryTable.setWidths(new float[]{1f, 1f});

            addSummaryCell(summaryTable, "Account Holder:", account.getFirstName() + " " + account.getLastName(), labelFont, valueFont);
            addSummaryCell(summaryTable, "Account Number:", account.getAccountNumber(), labelFont, valueFont);
            addSummaryCell(summaryTable, "Account Type:", account.getAccountType().toString(), labelFont, valueFont);
            addSummaryCell(summaryTable, "Current Balance:", "Rs. " + String.format("%.2f", account.getBalance()), labelFont, valueFont);
            addSummaryCell(summaryTable, "Account Status:", account.getStatus().toString(), labelFont, valueFont);
            addSummaryCell(summaryTable, "Statement Generated On:", java.time.LocalDateTime.now().format(DATE_FORMATTER), labelFont, valueFont);

            document.add(summaryTable);

            // 3. Transactions Section
            Paragraph transSectionTitle = new Paragraph("Transaction Logs", sectionTitleFont);
            transSectionTitle.setSpacingAfter(10);
            document.add(transSectionTitle);

            if (transactions.isEmpty()) {
                Paragraph noTrans = new Paragraph("No transactions found for this account.", valueFont);
                noTrans.setSpacingAfter(20);
                document.add(noTrans);
            } else {
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setSpacingAfter(20);
                table.setWidths(new float[]{2.5f, 2f, 3f, 1.8f, 2f, 1.7f}); // proportional widths

                // Table Headers
                String[] headers = {"Date & Time", "ID", "Counterparty", "Type", "Amount", "Status"};
                for (String header : headers) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(header, tableHeaderFont));
                    headerCell.setBackgroundColor(new Color(30, 41, 59));
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    headerCell.setPadding(6);
                    headerCell.setBorderColor(new Color(51, 65, 85));
                    table.addCell(headerCell);
                }

                // Table Rows
                boolean isEven = false;
                for (TransactionLogResponse tx : transactions) {
                    Color rowBg = isEven ? new Color(248, 250, 252) : Color.WHITE;
                    isEven = !isEven;

                    // Date & Time
                    PdfPCell cellDate = new PdfPCell(new Phrase(tx.createdOn().format(DATE_FORMATTER), tableBodyFont));
                    cellDate.setBackgroundColor(rowBg);
                    cellDate.setPadding(6);
                    cellDate.setBorderColor(new Color(226, 232, 240));
                    table.addCell(cellDate);

                    // ID
                    String displayId = tx.id().length() > 8 ? tx.id().substring(0, 8) + "..." : tx.id();
                    PdfPCell cellId = new PdfPCell(new Phrase(displayId, tableBodyFont));
                    cellId.setBackgroundColor(rowBg);
                    cellId.setPadding(6);
                    cellId.setBorderColor(new Color(226, 232, 240));
                    table.addCell(cellId);

                    // Counterparty
                    boolean isDebit = tx.fromAccountNumber().equals(account.getAccountNumber());
                    String counterparty = isDebit ? "To: " + tx.toAccountNumber() : "From: " + tx.fromAccountNumber();
                    PdfPCell cellCounter = new PdfPCell(new Phrase(counterparty, tableBodyFont));
                    cellCounter.setBackgroundColor(rowBg);
                    cellCounter.setPadding(6);
                    cellCounter.setBorderColor(new Color(226, 232, 240));
                    table.addCell(cellCounter);

                    // Type (DEBIT/CREDIT)
                    String type = isDebit ? "DEBIT" : "CREDIT";
                    Font typeFont = isDebit ? 
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(239, 68, 68)) : 
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 185, 129));
                    PdfPCell cellType = new PdfPCell(new Phrase(type, typeFont));
                    cellType.setBackgroundColor(rowBg);
                    cellType.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellType.setPadding(6);
                    cellType.setBorderColor(new Color(226, 232, 240));
                    table.addCell(cellType);

                    // Amount
                    String amountSign = isDebit ? "-" : "+";
                    String amountStr = amountSign + " Rs. " + String.format("%.2f", tx.amount());
                    Font amountFont = isDebit ? 
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(239, 68, 68)) : 
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 185, 129));
                    PdfPCell cellAmount = new PdfPCell(new Phrase(amountStr, amountFont));
                    cellAmount.setBackgroundColor(rowBg);
                    cellAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cellAmount.setPadding(6);
                    cellAmount.setBorderColor(new Color(226, 232, 240));
                    table.addCell(cellAmount);

                    // Status
                    Color statusColor = "SUCCESS".equalsIgnoreCase(tx.status()) ? new Color(16, 185, 129) : new Color(239, 68, 68);
                    Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, statusColor);
                    PdfPCell cellStatus = new PdfPCell(new Phrase(tx.status(), statusFont));
                    cellStatus.setBackgroundColor(rowBg);
                    cellStatus.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellStatus.setPadding(6);
                    cellStatus.setBorderColor(new Color(226, 232, 240));
                    table.addCell(cellStatus);
                }

                document.add(table);
            }

            // Footer / Disclaimer
            Paragraph disclaimer = new Paragraph("This is a system generated document and does not require a physical signature.", subtitleFont);
            disclaimer.setAlignment(Element.ALIGN_CENTER);
            disclaimer.setSpacingBefore(30);
            document.add(disclaimer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }
}
