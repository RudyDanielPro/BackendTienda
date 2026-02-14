package com.gestion.clientes.Servicios;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void enviarNotificacionAdmin(String listaProd, String links) {
        Resend resend = new Resend(resendApiKey);

        // Se cambió .htmlContent() por .html()
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("onboarding@resend.dev") 
                .to("rudydanielcarballo@gmail.com")
                .subject("🛍️ Nuevo Pedido - Confirmación de Stock Requerida")
                .html("<h3>Hola Admin,</h3>" +
                    "<p>Se ha iniciado un pedido por WhatsApp.</p>" +
                    "<strong>PRODUCTOS:</strong><br><pre>" + listaProd + "</pre><br>" +
                    "<strong>⚠️ PARA DESCONTAR EL STOCK:</strong><br>" + 
                    links.replace("\n", "<br>"))
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("✅ Correo enviado con éxito. ID: " + data.getId());
        } catch (Exception e) {
            System.err.println("❌ Error al enviar con Resend: " + e.getMessage());
            throw new RuntimeException("Error en el servicio de correo");
        }
    }
}