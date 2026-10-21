package com.fairshare.reminder.service;

import com.fairshare.reminder.dto.CreateReminderRequest;
import com.fairshare.reminder.dto.ReminderResponse;
import com.fairshare.reminder.model.PaymentReminder;
import com.fairshare.reminder.model.ReminderDelivery;
import com.fairshare.reminder.repository.PaymentReminderRepository;
import com.fairshare.reminder.repository.ReminderDeliveryRepository;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import com.fairshare.user.service.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ReminderService {
    private static final Set<Integer> INTERVALS=Set.of(7,10,15,30);
    private final PaymentReminderRepository reminders; private final ReminderDeliveryRepository deliveries;
    private final UserRepository users; private final WhatsAppDeliveryService whatsApp; private final InstagramDeliveryService instagram;
    public ReminderService(PaymentReminderRepository reminders,ReminderDeliveryRepository deliveries,UserRepository users,WhatsAppDeliveryService whatsApp,InstagramDeliveryService instagram){this.reminders=reminders;this.deliveries=deliveries;this.users=users;this.whatsApp=whatsApp;this.instagram=instagram;}

    @Transactional
    public ReminderResponse create(UUID ownerId,CreateReminderRequest request){
        String type=request.sourceType().trim().toLowerCase(Locale.ROOT);
        if(!Set.of("expense","request").contains(type))throw new BadRequestException("Reminder source must be expense or request.");
        if(!INTERVALS.contains(request.intervalDays()))throw new BadRequestException("Reminder interval must be 7, 10, 15, or 30 days.");
        User payer=request.payerUserId()==null?null:users.findById(request.payerUserId()).orElse(null);
        String phone=ContactService.normalizeMsisdn(request.payerMsisdn()!=null?request.payerMsisdn():payer==null?null:payer.getPhone());
        String name=request.payerName()!=null&&!request.payerName().isBlank()?request.payerName().trim():payer==null?"Payer":payer.getName();
        Set<String> channels=normalizeChannels(request.channels());
        if(channels.contains("whatsapp")&&phone.isBlank())throw new BadRequestException("A phone number is required for WhatsApp reminders.");
        PaymentReminder entity=new PaymentReminder(ownerId,type,request.sourceId(),request.expenseDescription().trim(),request.currency()==null?"INR":request.currency().toUpperCase(Locale.ROOT),request.amountMinor(),request.payerUserId(),name,phone,request.payerInstagramId(),channels,request.intervalDays());
        return ReminderResponse.from(reminders.save(entity));
    }

    @Transactional
    public ReminderResponse createForRequest(UUID ownerId,UUID requestId,String description,String currency,long amountMinor,UUID payerUserId,String payerName,String payerMsisdn,String payerInstagramId,Set<String> channels,Integer intervalDays){
        return createForRequest(ownerId,requestId,description,currency,amountMinor,payerUserId,payerName,payerMsisdn,payerInstagramId,channels,intervalDays,null,null);
    }

    @Transactional
    public ReminderResponse createForRequest(UUID ownerId,UUID requestId,String description,String currency,long amountMinor,UUID payerUserId,String payerName,String payerMsisdn,String payerInstagramId,Set<String> channels,Integer intervalDays,String paymentMode,String paymentInstructions){
        if(intervalDays==null)return null;
        if(!INTERVALS.contains(intervalDays))throw new BadRequestException("Reminder interval must be 7, 10, 15, or 30 days.");
        User payer=payerUserId==null?null:users.findById(payerUserId).orElse(null);
        String phone=ContactService.normalizeMsisdn(payerMsisdn!=null?payerMsisdn:payer==null?null:payer.getPhone());
        String name=payerName!=null&&!payerName.isBlank()?payerName.trim():payer==null?"Payer":payer.getName();
        Set<String> normalizedChannels=normalizeChannels(channels);
        if(normalizedChannels.contains("whatsapp")&&phone.isBlank())throw new BadRequestException("A phone number is required for WhatsApp reminders.");
        PaymentReminder entity=new PaymentReminder(ownerId,"request",requestId,description.trim(),currency==null?"INR":currency.toUpperCase(Locale.ROOT),amountMinor,payerUserId,name,phone,payerInstagramId,normalizedChannels,intervalDays,paymentMode,paymentInstructions);
        return ReminderResponse.from(reminders.save(entity));
    }

    @Transactional(readOnly=true)
    public List<ReminderResponse> list(UUID ownerId,String sourceType,UUID sourceId){
        List<PaymentReminder> result=sourceType!=null&&sourceId!=null?reminders.findByOwnerIdAndSourceTypeAndSourceIdOrderByCreatedAtDesc(ownerId,sourceType.toLowerCase(Locale.ROOT),sourceId):reminders.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        return result.stream().map(ReminderResponse::from).toList();
    }
    @Transactional public ReminderResponse changeStatus(UUID ownerId,UUID id,String status){PaymentReminder r=findOwned(ownerId,id);r.setStatus(status);return ReminderResponse.from(reminders.save(r));}
    @Transactional public void changeStatusForSource(UUID ownerId,String sourceType,UUID sourceId,String status){
        List<PaymentReminder> matches=reminders.findByOwnerIdAndSourceTypeAndSourceIdOrderByCreatedAtDesc(ownerId,sourceType,sourceId);
        matches.forEach(reminder->reminder.setStatus(status));
        reminders.saveAll(matches);
    }
    @Transactional public ReminderResponse sendNow(UUID ownerId,UUID id){PaymentReminder r=findOwned(ownerId,id);deliver(r);return ReminderResponse.from(reminders.save(r));}
    @Transactional public int deliverDue(){List<PaymentReminder> due=reminders.findByStatusAndNextSendAtLessThanEqual("active",Instant.now());due.forEach(this::deliver);reminders.saveAll(due);return due.size();}
    private void deliver(PaymentReminder r){
        String paymentCopy=r.getPaymentInstructions()==null||r.getPaymentInstructions().isBlank()?"":" Pay using "+r.getPaymentMode()+": "+r.getPaymentInstructions()+".";
        String message="Hi "+r.getPayerName()+", a friendly Fairshare reminder: ₹"+String.format(Locale.ROOT,"%.2f",r.getAmountMinor()/100.0)+" is pending for “"+r.getExpenseDescription()+"”."+paymentCopy+" If you use Fairshare, attach your transaction ID and payment screenshot after paying.";
        List<String> statuses=new ArrayList<>();
        for(String channel:r.getChannels()){
            if("whatsapp".equals(channel)){
                WhatsAppDeliveryService.DeliveryResult result=whatsApp.send(r.getPayerMsisdn(),message);
                statuses.add(result.status());deliveries.save(new ReminderDelivery(r.getId(),"whatsapp",r.getPayerMsisdn(),result.status(),result.providerReference(),result.errorMessage()));
            }else if("instagram".equals(channel)){
                InstagramDeliveryService.DeliveryResult result=instagram.send(r.getPayerInstagramId(),message);
                statuses.add(result.status());deliveries.save(new ReminderDelivery(r.getId(),"instagram",r.getPayerInstagramId(),result.status(),result.providerReference(),result.errorMessage()));
            }
        }
        r.setLastSentAt(Instant.now());r.setLastDeliveryStatus(aggregateStatus(statuses));r.setNextSendAt(Instant.now().plus(r.getIntervalDays(),ChronoUnit.DAYS));
    }
    private Set<String> normalizeChannels(Set<String> requested){
        Set<String> normalized=new LinkedHashSet<>();
        if(requested!=null)requested.stream().filter(value->value!=null&&!value.isBlank()).map(value->value.trim().toLowerCase(Locale.ROOT)).forEach(normalized::add);
        if(normalized.isEmpty())normalized.add("whatsapp");
        if(!Set.of("whatsapp","instagram").containsAll(normalized))throw new BadRequestException("Reminder channels must be WhatsApp, Instagram, or both.");
        return normalized;
    }
    private String aggregateStatus(List<String> statuses){
        if(statuses.isEmpty())return "failed";
        if(statuses.stream().allMatch("sent"::equals))return "sent";
        if(statuses.stream().anyMatch("failed"::equals))return statuses.stream().anyMatch("sent"::equals)?"partial_failed":"failed";
        if(statuses.stream().anyMatch("recipient_link_required"::equals))return "recipient_link_required";
        return "queued_configuration_required";
    }
    private PaymentReminder findOwned(UUID ownerId,UUID id){return reminders.findById(id).filter(r->r.getOwnerId().equals(ownerId)).orElseThrow(()->new ResourceNotFoundException("Reminder not found."));}
}
