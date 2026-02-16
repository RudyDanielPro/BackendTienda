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

    // Se agrega el parámetro 'String asunto' para permitir títulos dinámicos
    public void enviarNotificacionAdmin(String asunto, String listaProd, String links) {
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("onboarding@resend.dev") 
                .to("sadysanchez1980@gmail.com")
                .subject(asunto) // Ahora usa el asunto generado dinámicamente
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