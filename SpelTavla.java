    // De nödvändiga biblioteken för spelets grafisk hantering, händelser, nätverkskommunikation och fönsterkomponenter
    import java.awt.*;
    import java.awt.event.*;
    import java.io.IOException;
    import java.net.*;
    import javax.swing.*;

    // Klassen SpelTavla ärver från JFrame och är huvudpresentationen av spelet
    public class SpelTavla extends JFrame {

        // Dessa fält är för att hålla spelets spelområde, nätverkskommunikation och information om spelare
        private SpelOmrade spelOmråde;
        // UDP socket för nätverkskommunikation
        private DatagramSocket uttag;   
        private int port;
        private String fjärrVärd;
        private int fjärrPort;
        private JLabel spelarNamnJLabel;
        private JLabel spelarSymbolJLabel;
        private char minSymbol;

        // Konstruktor för SpelTavla och tilldelning
        public SpelTavla(int port, String fjärrVärd, int fjärrPort, String spelarNamn) throws IOException {
            this.port = port;
            this.fjärrVärd = fjärrVärd;
            this.fjärrPort = fjärrPort;

            // Tanken här är att bestäm vilken symbol spelaren får baserat på portnumret
            this.minSymbol = (port < fjärrPort) ? 'X' : 'O';

            // Vi skapar etiketter för spelarens namn och symbol
            spelarNamnJLabel = new JLabel("Hej " + spelarNamn + ". Välkommen till Tik-Tak-Toe spelet!");
            spelarNamnJLabel.setFont(new Font("Arial", Font.BOLD, 22));
            spelarSymbolJLabel = new JLabel("Du spelar som: " + minSymbol);
            spelarSymbolJLabel.setFont(new Font("Arial", Font.BOLD, 20));

            // Vi skapar spelområdet
            spelOmråde = new SpelOmrade();

            // Lägger till komponenter i fönstret
            setLayout(new BorderLayout());
            JPanel överPanel = new JPanel(new GridLayout(2, 1));
            överPanel.add(spelarNamnJLabel);
            överPanel.add(spelarSymbolJLabel);

            add(överPanel, BorderLayout.NORTH);
            add(spelOmråde, BorderLayout.CENTER);

            // Grundläggande konfiguration för fönstret
            setTitle("Tik-Tak-Toe");
            setSize(600, 600);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setVisible(true);
            setLocationRelativeTo(null);
            setResizable(false);

            // Skapa nätverksuttag och starta lyssnartråd
            uttag = new DatagramSocket(this.port);
            new Thread(this::taEmotData).start(); // Här anropar vi taEmotData i en egen tråd // Lambda

        }

        // Den här metoden är viktig, lyssnar ständigt efter inkommande drag från motståndaren
        private void taEmotData() {
            byte[] buffert = new byte[1024];
            DatagramPacket paket = new DatagramPacket(buffert, buffert.length);

            while (true) {
                try {
                    uttag.receive(paket);
                    String meddelande = new String(paket.getData(), 0, paket.getLength());
                    String[] delar = meddelande.split(",");
                    int rad = Integer.parseInt(delar[0]);
                    int kolumn = Integer.parseInt(delar[1]);
                    // Skicka draget till spelområdet
                    spelOmråde.taEmotDrag(rad, kolumn); 
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Detta är den inre klassen som hanterar själva spelplanen
        class SpelOmrade extends JPanel {
            // Vi gör 3x3 spelplanen
            private char[][] board = new char[3][3]; 
            // X-spelaren börjar alltid
            private boolean minTur = (minSymbol == 'X'); 

            // Konstruktor och konfigurationer för spelplanen
            public SpelOmrade() {
                setBackground(Color.WHITE);
                setPreferredSize(new Dimension(600, 600));
                setLocationRelativeTo(null); 
                setResizable(false);

                // Vi lägger till en muslyssnare för att reagera på klick
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (!minTur) {
                            JOptionPane.showMessageDialog(rootPane, "Det är inte din tur!");
                            return;
                        }

                        int rad = e.getY() / (getHeight() / 3);
                        int kolumn = e.getX() / (getWidth() / 3);

                        if (board[rad][kolumn] == '\0') { // Om rutan är tom
                            board[rad][kolumn] = minSymbol;
                            repaint();
                            skickaDrag(rad, kolumn);
                            minTur = false;
                            kontrolleraVinst(minSymbol);
                        }
                    }
                });
            }

            // Denna metoden är för att måla spelplanen
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                // Vi gör linjerna tjockare för att det ska se lite tydligare och bättre ut 
                g2.setStroke(new BasicStroke(5)); 
                int w = getWidth();
                int h = getHeight();

                // Här ritas rutnätet
                g2.setColor(Color.BLACK);
                g2.drawLine(w / 3, 0, w / 3, h);
                g2.drawLine(2 * w / 3, 0, 2 * w / 3, h);
                g2.drawLine(0, h / 3, w, h / 3);
                g2.drawLine(0, 2 * h / 3, w, 2 * h / 3);

                // Här ritas X och O på spelplanen
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        if (board[r][c] == 'X') {
                            g2.setColor(Color.BLUE);
                            g2.drawLine(c * w / 3 + 20, r * h / 3 + 20, (c + 1) * w / 3 - 20, (r + 1) * h / 3 - 20);
                            g2.drawLine((c + 1) * w / 3 - 20, r * h / 3 + 20, c * w / 3 + 20, (r + 1) * h / 3 - 20);
                        } else if (board[r][c] == 'O') {
                            g2.setColor(Color.RED);
                            g2.drawOval(c * w / 3 + 20, r * h / 3 + 20, w / 3 - 40, h / 3 - 40);
                        }
                    }
                }
            }

            // Skickar ett drag till motståndaren via nätverket
            private void skickaDrag(int rad, int kolumn) {
                String meddelande = rad + "," + kolumn;
                byte[] data = meddelande.getBytes();
                try {
                    DatagramPacket paket = new DatagramPacket(data, data.length, InetAddress.getByName(fjärrVärd), fjärrPort);
                    uttag.send(paket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Här tar vi emot motståndarens drag och uppdaterar vår spelplan med det draget.
            public void taEmotDrag(int rad, int kolumn) {
                char motståndarSymbol = (minSymbol == 'X') ? 'O' : 'X';
                if (board[rad][kolumn] == '\0') {
                    board[rad][kolumn] = motståndarSymbol;
                    repaint();
                    // Sen blir det vår tur efter uppdateringen.
                    minTur = true;
                    kontrolleraVinst(motståndarSymbol);
                }
            }

            // Här sker kontroll över: om någon har vunnit eller om det är oavgjort
            private void kontrolleraVinst(char spelare) {
                
                // Vi Kolla rader och kolumner
                for (int i = 0; i < 3; i++) {
                    if (board[i][0] == spelare && board[i][1] == spelare && board[i][2] == spelare ||
                        board[0][i] == spelare && board[1][i] == spelare && board[2][i] == spelare) {
                        visaVinst(spelare);
                        return;
                    }
                }

                // Kolla diagonaler
                if (board[0][0] == spelare && board[1][1] == spelare && board[2][2] == spelare ||
                    board[0][2] == spelare && board[1][1] == spelare && board[2][0] == spelare) {
                    visaVinst(spelare);
                    return;
                }

                // Här kollar vi om det är fullt på brädet och om det är blir det oavgjort och ett meddelande visas (Oavgjort!)
                boolean fullt = true;
                for (char[] rad : board) {
                    for (char ruta : rad) {
                        if (ruta == '\0') fullt = false;
                    }
                }
                if (fullt) {
                    JOptionPane.showMessageDialog(this, "Oavgjort!");
                    resetSpel();
                }
            }

            // Denna metod är för att visa ett meddelande om vem som vann
            private void visaVinst(char spelare) {
                if (spelare == minSymbol) {
                    JOptionPane.showMessageDialog(this, "Du vann, grattis!");
                } else {
                    JOptionPane.showMessageDialog(this, "Din motståndare vann!");
                }
                resetSpel();
            }

            // Starta om spelet
            private void resetSpel() {
                board = new char[3][3];
                repaint();
                // Det är X som börjar alltid
                minTur = (minSymbol == 'X'); 
            }
        }

        // Programstart
        public static void main(String[] args) throws IOException {
            // Vi Kontrollera att fyra argument skickas med för att kunna köra spelet annars visas meddelandet
            if (args.length != 4) {
                System.out.println("För att köra programmet skriv: java SpelTavla <lokalPort> <fjärrVärd> <fjärrPort> <spelarNamn>");
                System.exit(1);
            }

            int lokalPort = Integer.parseInt(args[0]);
            String fjärrVärd = args[1];
            int fjärrPort = Integer.parseInt(args[2]);
            String spelarNamn = args[3];

            // Här startas spelet
            new SpelTavla(lokalPort, fjärrVärd, fjärrPort, spelarNamn);
        }
    }




