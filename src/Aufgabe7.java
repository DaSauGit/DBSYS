import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class Aufgabe7 extends JFrame{
    JComboBox<String> land;
    JComboBox<String> ausstattung;
    JTextField anreisedatum;
    JTextField abreisedatum;
    static Connection conn;
    static Statement stmt;

    List ausgewaehlteFerienwohnungen = new List();

    public Aufgabe7() {
        this.setTitle("Ferienwohnungen Suchen");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] laender = {"Deutschland", "Frankreich", "Italien", "Spanien", "Schweiz", "Österreich"};
        String[] ausstattungen = { "-","Balkon", "Sauna", "TV", "WLAN", "Küche"};


        JLabel landLabel = new JLabel("Land:");
        land = new JComboBox<>(laender);
        JLabel ausstattungLabel = new JLabel("Ausstattung:");
        ausstattung = new JComboBox<>(ausstattungen);

        JLabel anreiseLabel = new JLabel("Anreise Datum:");
        JLabel abreiseLabel = new JLabel("Abreise Datum:");
        anreisedatum = new JTextField(LocalDate.now().plusDays(1).toString(),10);
        abreisedatum = new JTextField(LocalDate.now().plusDays(4).toString(),10);

        JButton suchButton = new JButton("Suchen");
        suchButton.addActionListener(e -> {
            try {
                StringBuilder sql = new StringBuilder("SELECT f.Ferienwohnungsname, AVG(b.Sterne) AS Durchschnittsbewertung " +
                        "FROM dbsys43.Ferienwohnung f " +
                        "JOIN dbsys43.Adresse a  ON f.AdresseID = a.AdresseID " +
                        "JOIN dbsys43.Land l     ON a.LandName = l.LandName " +
                        "JOIN dbsys43.Besitzt be ON f.Ferienwohnungsname = be.Ferienwohnungsname " +
                        "LEFT JOIN dbsys43.Buchung b ON f.Ferienwohnungsname = b.Ferienwohnungsname " +
                        "WHERE l.LandName = ? "
                        );

                if (ausstattung.getSelectedItem() != "-") {
                    sql.append("AND   be.Ausstattungsname = ? ");
                }

                sql.append("AND   f.Ferienwohnungsname NOT IN ( " +
                        "        SELECT f2.Ferienwohnungsname " +
                        "        FROM dbsys43.Buchung b2 " +
                        "        JOIN dbsys43.Ferienwohnung f2 ON b2.Ferienwohnungsname = f2.Ferienwohnungsname " +
                        "        WHERE ( b2.Startdatum BETWEEN ? AND ? " +                    // 3,4
                        "                OR b2.Enddatum   BETWEEN ? AND ? " +                 // 5,6
                        "                OR ( b2.Startdatum <= ? AND b2.Enddatum >= ? ) ) " + // 7,8
                        "      ) " +
                        "GROUP BY f.Ferienwohnungsname " +
                        "ORDER BY AVG(b.Sterne) DESC NULLS LAST");


                PreparedStatement ps = conn.prepareStatement(String.valueOf(sql));

                int counter = 1;
                ps.setString(counter, land.getSelectedItem().toString());
                counter++;
                if (ausstattung.getSelectedItem() != "-") {
                    ps.setString(counter, ausstattung.getSelectedItem().toString());
                    counter++;
                }
                String anreiseString = anreisedatum.getText();
                String abreiseString = abreisedatum.getText();

                ps.setDate(counter, Date.valueOf(anreiseString));
                counter++;
                ps.setDate(counter, Date.valueOf(abreiseString));
                counter++;
                ps.setDate(counter, Date.valueOf(anreiseString));
                counter++;
                ps.setDate(counter, Date.valueOf(abreiseString));
                counter++;
                ps.setDate(counter, Date.valueOf(anreiseString));
                counter++;
                ps.setDate(counter, Date.valueOf(abreiseString));

                ResultSet rs = ps.executeQuery();
                ausgewaehlteFerienwohnungen.removeAll();
                while (rs.next()) {
                    String r1 = rs.getString("ferienwohnungsname");
                    ausgewaehlteFerienwohnungen.add(r1);
                    double schnitt = rs.getDouble("Durchschnittsbewertung");
                    System.out.printf("%-30s  %.2f Sterne%n", r1, schnitt);
                }

                int maxBuchungsnummer = 0;
                String sqlMax =
                        "SELECT MAX(buchungsnummer) AS max_bn " +
                                "FROM ( " +
                                "  SELECT buchungsnummer FROM dbsys43.buchung " +
                                "  UNION ALL " +
                                "  SELECT buchungsnummer FROM dbsys43.storniertebuchung " +
                                ") x";

                try (PreparedStatement psMax = conn.prepareStatement(sqlMax);
                     ResultSet rsMax = psMax.executeQuery()) {

                    if (rsMax.next()) {
                        maxBuchungsnummer = rsMax.getInt("max_bn");   // dank Alias bequem auszulesen
                    }
                }

                String s = (String) JOptionPane.showInputDialog(
                        this,
                        "Wählen sie die gewünschte Ferienwohnung aus",
                        "Ferienwohnung wählen",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        ausgewaehlteFerienwohnungen.getItems(),
                        ausgewaehlteFerienwohnungen.getItem(0)
                );

                String sqlBuchung = null;
                if (s != null) {
                    String sqlInsert =
                            "INSERT INTO dbsys43.buchung (" +
                                    "  Buchungsnummer, Ferienwohnungsname, Mail, Datum, Enddatum, Startdatum," +
                                    "    Stornierungsdatum, Sterne, Bewertungsdatum, Rechnungsnummer, Rechnungsbetrag, Rechnungsdatum" +
                                    ") VALUES (?,?,?,?,?,?,?,?,?,?,?, ?)";

                    try (PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
                        psIns.setInt   (1, maxBuchungsnummer + 1);
                        psIns.setString(2, s);
                        psIns.setString(3, "max.mustermann@mail.de");
                        psIns.setDate  (4, Date.valueOf(LocalDate.now()));
                        psIns.setDate  (5, Date.valueOf(abreiseString));
                        psIns.setDate  (6, Date.valueOf(anreiseString));
                        psIns.setNull  (7, Types.DATE);
                        psIns.setNull(8, Types.FLOAT);
                        psIns.setNull  (9, Types.DATE);// Beispielwert
                        psIns.setInt   (10, maxBuchungsnummer + 1001);
                        psIns.setBigDecimal(11, new BigDecimal("800.00"));
                        psIns.setDate  (12, Date.valueOf(LocalDate.now()));

                        int rows = psIns.executeUpdate();
                        System.out.println(rows + " Buchung(en) eingefügt");
                    }

                }

                rs.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    conn.close(); stmt.close();
                } catch (SQLException ex) { }
            }
        });

        JPanel jpanel = new JPanel();
        jpanel.setLayout(new GridLayout(5,2));
        jpanel.add(landLabel);
        jpanel.add(land);
        jpanel.add(ausstattungLabel);
        jpanel.add(ausstattung);
        jpanel.add(anreiseLabel);
        jpanel.add(anreisedatum);
        jpanel.add(abreiseLabel);
        jpanel.add(abreisedatum);
        jpanel.add(suchButton);
        jpanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(jpanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    public static void main(String[] args) {
        JFrame myAppl = new Aufgabe7();

        String url = "jdbc:oracle:thin:@oracle19c.in.htwg-konstanz.de:1521:ora19c";
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            conn = DriverManager.getConnection(url, "dbsys64", "dbsys64");
            stmt = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
