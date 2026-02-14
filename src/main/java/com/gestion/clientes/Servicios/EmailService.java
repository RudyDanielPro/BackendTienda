package com.gestion.clientes.Servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarNotificacionAdmin(String listaProd, String links) {
        SimpleMailMessage email = new SimpleMailMessage();
        
        // -----------------------------------------------------------
        // CAMBIO AQUÍ: Pon el correo de la persona que debe RECIBIR el aviso
        // No pongas el mismo correo desde el que se envía.
        // -----------------------------------------------------------
        email.setTo("rudydanielcarballo@gmail.com"); 
        
        email.setSubject("🛍️ Nuevo Pedido - Confirmación de Stock Requerida");
        email.setText("Hola Admin,\n\nSe ha iniciado un pedido por WhatsApp.\n\n" +
                      "PRODUCTOS:\n" + listaProd + "\n\n" +
                      "⚠️ PARA DESCONTAR EL STOCK DE ESTA VENTA, HAZ CLIC EN LOS LINKS:\n\n" + links);
        
        mailSender.send(email);
    }
}