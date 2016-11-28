package ee.ria.dhx.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import javax.persistence.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


/**
 * The persistent class for the transport database table.
 * 
 */
@Entity
@Getter
@Table(name = "transport")
@Setter
public class Transport  extends BaseEntity implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "transport_id")
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Integer transportId;

  @Column(name = "saatmise_algus")
  private Timestamp sendingStart;

  @Column(name = "saatmise_lopp")
  private Timestamp sendingEnd;

  // bi-directional many-to-one association to Saatja
  @OneToMany(mappedBy = "transport", cascade = {CascadeType.ALL})
  private List<Sender> senders;

  // bi-directional many-to-one association to Dokument
  @ManyToOne
  @JoinColumn(name = "dokument_id")
  private Document dokument;

  // bi-directional many-to-one association to Klassifikaator
  @ManyToOne
  @JoinColumn(name = "staatus_id")
  private Classificator status;

  // bi-directional many-to-one association to Vastuvotja
  @OneToMany(mappedBy = "transport", cascade = {CascadeType.ALL})
  private List<Recipient> recipients;

  public Transport() {}


  public Sender addSender(Sender sender) {
    if (getSenders() == null) {
      setSenders(new ArrayList<Sender>());
    }
    getSenders().add(sender);
    sender.setTransport(this);

    return sender;
  }

  public Sender removeSender(Sender sender) {
    getSenders().remove(sender);
    sender.setTransport(null);

    return sender;
  }

  public Recipient addRecipient(Recipient recipient) {
    if (getRecipients() == null) {
      setRecipients(new ArrayList<Recipient>());
    }
    getRecipients().add(recipient);
    recipient.setTransport(this);

    return recipient;
  }

  public Recipient removeVastuvotja(Recipient recipient) {
    getRecipients().remove(recipient);
    recipient.setTransport(null);

    return recipient;
  }

}
