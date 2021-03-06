<%@ page language="java" contentType="text/html; charset=utf-8"%>
<%@ include file="/WEB-INF/content/layout/taglibs.jsp"%>

<div class="body">
	<s:hidden theme="simple" id="pageName" value="page-cloth-distribute"/>
	
	<s:form theme="simple" id="addFrom" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="post" cssClass="ajaxForm">
	<s:hidden theme="simple" id="hiddenKioskName" name="kioskName"/>
	
<!-- ################################################## -->
<!-- Receipt Info Fieldset -->
<!-- ################################################## -->
		<fieldset id="receiptInfoFieldset" name="receiptInfoFieldset" class="kioskFieldset fieldsetStyle01">
		
			<legend><s:text name="fieldset.legend.cloth.distribution"/></legend>
			
			<!-- This div's ref must set to form id -->
			<div class="actionErrors" ref="addFrom"></div>
			<div class="actionMessages" ref="addFrom"></div>
			
			<div class="alignRight">
				<s:submit theme="simple" type="button" id="ajaxBtnReceiptSave" key="btn.finish" method="create" cssClass="kioskButton blue buttonMarginCorner ajaxButton" />
				<s:submit theme="simple" type="button" id="ajaxBtnReceiptReset" key="btn.reset" method="XXXXXXX" cssClass="kioskButton rosy buttonMarginCorner" />
			</div>
			
			
			<ul>
				<li>
					<label for="receipt.code"><s:text name="receipt.code"/>:</label>
					<s:textfield theme="simple" name="receipt.code" cssClass="displayText" readonly="true"/>
				</li>
				
				<li>
					<label for="staffCardNumberHandledBy"><s:text name="staff.card.handled.by"/>: </label>
					<s:textfield theme="simple" id="staffCardNumberHandledBy" name="staffHandledBy.cardNumber" cssClass="inputText"  />
				</li>
				
				<li>
					<label for="staffCodeHandledBy"><s:text name="staff.code.handled.by"/>:</label>
					<s:textfield theme="simple" id="staffCodeHandledBy" name="staffHandledBy.code" cssClass="inputText" />
					<img id="staffCodeHandledByMicrosoftVirtualKeyboard" src="<s:property value="imagesPath"/>layout/Keyboard.png" style="visibility:hidden" />
				</li>
				
				<li>
					<label for="staffCardNumberPickedBy"><s:text name="staff.card.picked.by"/>: </label>
					<s:textfield theme="simple" id="staffCardNumberPickedBy" name="staffPickedBy.cardNumber" cssClass="inputText"  />
				</li>
				
				<li>
					<label for="staffCodePickedBy"><s:text name="staff.code.picked.by"/>:</label>
					<s:textfield theme="simple" id="staffCodePickedBy" name="staffPickedBy.code" cssClass="inputText" />
					<img id="staffCodePickedByMicrosoftVirtualKeyboard" src="<s:property value="imagesPath"/>layout/Keyboard.png" style="visibility:hidden" />
				</li>
				
				<li>
					<label for="receiptRemark"><s:text name="receipt.remark"/>:</label>
					<s:textarea theme="simple" id="receiptRemark" name="receipt.remark"/>
					<img id="receiptRemarkMicrosoftVirtualKeyboard" src="<s:property value="imagesPath"/>layout/Keyboard.png" style="visibility:hidden" />
				</li>
			</ul>
		</fieldset>
	</s:form>
	
	<div class="buttonArea">
		<s:submit theme="simple" type="button" id="btnCaptureStart" key="btn.capture" method="XXXX" cssClass="kioskButton blue buttonMargin" />
		<s:submit theme="simple" type="button" id="btnCaptureStop" key="btn.capture.stop" method="XXXXXX" cssClass="kioskButton rosy buttonMargin" />
	</div>
	
<!-- ################################################## -->
<!-- RFID Capture Summary Fieldset -->
<!-- ################################################## -->
	<fieldset id="rfidCaptureSummaryFieldset" name="rfidCaptureSummaryFieldset" class="kioskFieldset fieldsetStyle02">
		<legend><s:text name="fieldset.legend.rfid.capture.summary"/></legend>
		
		<div class="RFIDTotal">
			<!-- RFID Summary Table -->
			<table id="RFIDTotal">
				<thead>
					<tr><td><s:text name="label.cloth.total"/></td></tr>
				</thead>
				
				<tbody>
					<tr>
						<td class="bigText">
							<s:property value="clothTotal"/>
						</td>
					</tr>
				</tbody>
				
				<tfoot></tfoot>
			</table>
		</div>
				
		<div class="RFIDSummary">
			<table id="RFIDSummary">
				<thead>
					<tr>
						<td><s:text name="clothType.clothType"/></td>
						<td><s:text name="label.type.total"/></td>
					</tr>
				</thead>
				
				<tbody>
					<s:iterator value="clothTypeCountList">
						<tr>
							<td><s:property value="type"/></td>
							<td><s:property value="num"/></td>
						</tr>
					</s:iterator>
					
					<!-- Auto Generated -->
				</tbody>
			
			<tfoot></tfoot>
			</table>	
			<!-- End of RFID Summary Table -->
		</div>
	</fieldset>

<!-- ################################################## -->
<!-- RFID Capture Detail Fieldset -->
<!-- ################################################## -->
	<fieldset id="rfidCaptureDetailFieldset" name="rfidCaptureDetailFieldset" class="kioskFieldset fieldsetStyle01">
		<legend><s:text name="fieldset.legend.rfid.capture.detail"/></legend>
		
		<div class="alignRight">
			<s:url var="removeClothUrl" action="cloth-distribute-fixed-reader" method="ajaxRemoveRfid"></s:url>
			<s:submit theme="simple" 
				type="button" 
				id="ajaxBtnRemoveCloth" 
				key="btn.remove" 
				cssClass="kioskButton rosy buttonMarginCorner" 
				ref="rfidCaptureDetailFieldset"
				url="#removeClothUrl" />
		</div>
		
		<div class="actionErrors" ref="rfidCaptureDetailFieldset"></div>
		<div class="actionMessages" ref="rfidCaptureDetailFieldset"></div>
		
		<div class="kioskRFIDTable">
			<!-- Retrieve Data Table -->
			<table id="RFIDTable" class="dataTable">
				<thead>
					<tr>
					
						<th class="simpleCheckBox">
							<s:checkbox theme="simple" 
								id="checkAllDataRows" 
								name="checkAllDataRows" 
								cssClass="simpleCheckBox checkall" 
								ref="checkAllDataRows">
							</s:checkbox>
						</th>
						<th><s:text name="staff.dept"/></th>
						<th><s:text name="staff.code"/></th>
						<th><s:text name="staff.name"/></th>
						<th><s:text name="clothType.clothType"/></th>
						<th><s:text name="cloth.code"/></th>
						<th><s:text name="label.rfid"/></th>
						<th><s:text name="cloth.location"/></th>
					</tr>
				</thead>
				
				<tbody>
					<!-- Auto Generated! -->
				</tbody>
				
				<tfoot></tfoot>
			</table>
			<!-- End of Retrieve Data Table -->
		</div>
	</fieldset>
</div>



<script>

$(function() {
	
	var hiddenKioskName = $("#hiddenKioskName").val();
	// alert("hiddenKioskName: " + hiddenKioskName);
	$("#staffCardNumberHandledBy").focus();
	
	twoRadioedTextFieldsDefault(
			$("#staffCardNumberHandledBy"), 
			function(){ 
				$("#staffCodeHandledBy").val(""); 
			},
			$("#staffCodeHandledBy"), 
			function(){ 
				$("#staffCardNumberHandledBy").val(""); 
			} 
	);

	twoRadioedTextFieldsDefault(
			$("#staffCardNumberPickedBy"), 
			function(){ 
				$("#staffCodePickedBy").val(""); 
			},
			$("#staffCodePickedBy"), 
			function(){ 
				$("#staffCardNumberPickedBy").val(""); 
			} 
	);	

	listenToSupportMicrosoftVirtualKeyboard( $("#staffCodeHandledBy"), $("#staffCodeHandledByMicrosoftVirtualKeyboard") );
	listenToSupportMicrosoftVirtualKeyboard( $("#staffCodePickedBy"), $("#staffCodePickedByMicrosoftVirtualKeyboard") );
	listenToSupportMicrosoftVirtualKeyboard( $("#receiptRemark"), $("#receiptRemarkMicrosoftVirtualKeyboard") );
	
	<s:url var="startCaptureUrl" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="startCapture"></s:url>
	$("#btnCaptureStart").on("click", function() {
		$.ajax({
			url: '<s:property value="#startCaptureUrl"/>',
			data: {"kioskName": hiddenKioskName},
			success: function()
			{
				$("#btnCaptureStart").attr("disabled", true);
				$("#btnCaptureStart").addClass("disableButton");
				$("#btnCaptureStart").removeClass("blue");
				
				$("#btnCaptureStop").attr("disabled", false);
				$("#btnCaptureStop").addClass("rosy");
		 		$("#btnCaptureStop").removeClass("disableButton");
				
				$("#ajaxBtnReceiptSave").attr("disabled", true);
		 		$("#ajaxBtnReceiptSave").addClass("disableButton");
		 		$("#ajaxBtnReceiptSave").removeClass("blue");

				$("#ajaxBtnReceiptReset").attr("disabled", true);
				$("#ajaxBtnReceiptReset").addClass("disableButton");
				$("#ajaxBtnReceiptReset").removeClass("rosy");
				
				$("#ajaxBtnRemoveCloth").attr("disabled", true);
		 		$("#ajaxBtnRemoveCloth").addClass("disableButton");
		 		$("#ajaxBtnRemoveCloth").removeClass("rosy");
				
				// Very import to call worker() here because worker() check disabled status of btn #btnCaptureStart
				worker();
			},
			complete: function()
			{
			}
		});
		
		return false;
	});
	
	<s:url var="stopCaptureUrl" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="stopCapture"></s:url>
	$("#btnCaptureStop").on("click", function() {
		
		$.ajax({
			url: '<s:property value="#stopCaptureUrl"/>',
			data: {"kioskName": hiddenKioskName},
			success: function()
			{
				$("#btnCaptureStart").attr("disabled", false);
		 		$("#btnCaptureStart").removeClass("disableButton");
		 		$("#btnCaptureStart").addClass("blue");
				
				$("#btnCaptureStop").attr("disabled", true);
		 		$("#btnCaptureStop").addClass("disableButton");
		 		$("#btnCaptureStop").removeClass("rosy");
				
				$("#ajaxBtnReceiptSave").attr("disabled", false);
				$("#ajaxBtnReceiptSave").addClass("blue");
		 		$("#ajaxBtnReceiptSave").removeClass("disableButton");

				$("#ajaxBtnReceiptReset").attr("disabled", false);
				$("#ajaxBtnReceiptReset").addClass("rosy");
		 		$("#ajaxBtnReceiptReset").removeClass("disableButton");
				
				$("#ajaxBtnRemoveCloth").attr("disabled", false);
				$("#ajaxBtnRemoveCloth").addClass("rosy");
		 		$("#ajaxBtnRemoveCloth").removeClass("disableButton");
			},
			complete: function()
			{
			}
		});
		
		return false;
	});
	
	
	//////////////////////////////////////////////////////////////////
	// Ensure the connection between server and client never terminate
	//////////////////////////////////////////////////////////////////
	keepSessionAlive();
	<s:url var="keepSessionAliveUrl" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="keepSessionAlive"></s:url>
	function keepSessionAlive()
	{
		$.ajax({
			url: '<s:property value="#keepSessionAliveUrl"/>',
			data: { "kioskName": hiddenKioskName },
			success: function(data) {
				// Nothing to do
			},
			complete: function() {
				setTimeout(keepSessionAlive, 600000);	// call every 10 minutes
			}
		});
	}
	
	
	<s:url var="getCapturedRfidJsonUrl" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="getCapturedRfidJson"></s:url>
	function worker() {
		$.ajax({
			url: '<s:property value="#getCapturedRfidJsonUrl"/>',
			data: { "kioskName": hiddenKioskName, "struts.enableJSONValidation": true },
			success: function(data) {
				
				//clear table tbody
		      	// $("#RFIDTable tbody").text("");
		      	
				// 1. append rows to RFIDTable
				$.each(data.clothList, function(key, value) {
					
					var row = $("<tr/>").attr("id", "RFIDTable_row_" + value.rfid);
					row.append(
						$("<td/>")
							.append(
								$("<input/>")
								.attr("id", value.rfid)
								.attr("type", "checkbox")
								.attr("name", "cb_rfid_" + value.rfid)
								.attr("checkall", "checkAllDataRows")
								.addClass("simpleCheckBox checkall2 rfidMaybeRemoveMarker") 
							).append(
								$("<input/>")
								.attr("id", "hidden_rfid_" + value.rfid)
								.attr("type", "hidden")
								.attr("name", "hidden_rfid_" + value.rfid)
								.val("" + value.rfid) ) 
					);
					
					row.append($("<td/>").text(value.staff.dept.nameCht));
					row.append($("<td/>").text(value.staff.code));
					row.append($("<td/>").text(value.staff.nameCht));
					row.append($("<td/>").text(value.clothType.name));
					row.append($("<td/>").text(value.code));
					row.append($("<td/>").addClass("RFIDDisplay").text(value.rfid));
					row.append($("<td/>").text(value.zone.code));
					
					$("#RFIDTable tbody").append(row);
				});
				
				// 2. update cloth total
				$("#RFIDTotal tbody tr td").text( data.clothTotal );
				
				
				// 3. refresh RFIDSummary table
				$("#RFIDSummary tbody").text("");
				$.each(data.clothTypeCountList, function(key, value) {
					
					var row = $("<tr/>");
					row.append($("<td/>").text(value.type));
					row.append($("<td/>").text(value.num));
					
					$("#RFIDSummary tbody").append(row);
				});
			},
			complete: function() {
				setTimeout(worker, 1000);
			}
		});
	}
	
	
	// To server: delete selected cloth from rfidFromMapRfidClothAll
	$("#ajaxBtnRemoveCloth").live('click', 
			
			function() {
				// check that at least one row must be selected 
				var n = $(".rfidMaybeRemoveMarker:checked").length;
				if (n == 0)
				{
					alertDialog(
							"", 
							"<s:text name="dialog.errors.nothing.select"/>", 
							"<s:text name="btn.ok"/>"
					);
					return false;
				}
				
				optionsAvailableMsgDialog(
					"", 
					"<s:text name="dialog.msg.confirm.ask"/>", 
					"<s:text name="btn.yes"/>",
					function()
					{
						//////////////////////////////////////////////////////////////////////////
						// 1. Preparing the data submit to Server, only checked row will submit
						//////////////////////////////////////////////////////////////////////////
						var j = 0;
						$(".rfidMaybeRemoveMarker").each(function() {
							if ($(this).attr("checked"))
							{
								var curRowIndex = $(this).attr("id");	// RFID as the row index
								$(this).attr("disabled", true);			// checkbox's value will not be submitted
								$("#hidden_rfid_" + curRowIndex).attr("disabled", false);	// ensure this hidden field's value will be submitted
								$("#hidden_rfid_" + curRowIndex).attr("name", "rfidToBeRemovedList[" + j + "]");
								j++;
							}
							else
							{
								var curRowIndex = $(this).attr("id");
								$(this).attr("disabled", true);
								$("#hidden_rfid_" + curRowIndex).attr("disabled", true);	// also disable this hidden field
							}
						});
						
						
						//////////////////////////////////////////////////////////////
						// 2. Request Server to remove the selected RFID at back end
						//////////////////////////////////////////////////////////////
						var ajaxBtnRemoveClothVar = $("#ajaxBtnRemoveCloth");
						var elementId = ajaxBtnRemoveClothVar.attr("ref");		// ref: fieldset id
						var triggerTarget = $("#" + elementId);
						var div = $("#" + elementId + " :input");	// unchecked checkboxs and their hidden field are disabled and won't be submitted 
						
						div.removeClass("fieldErrors");
						var url = ajaxBtnRemoveClothVar.attr("url");				// url: the method of ActionClass to be called
						var data = div.serialize();
						data += "&kioskName=" + hiddenKioskName;
						data += "&struts.enableJSONValidation=true";
						resetAllMessage(elementId);
						
						$.post(
								url,
								data,
								function(result) {
									
									if (result.errors == undefined && result.fieldErrors == undefined)
									{
										triggerTarget.trigger(elementId + ".success", result);
										removeActionMessageAndActionError(elementId);
										
										// 2.1. update the cloth total
										$("#RFIDTotal tbody tr td").text( result.clothTotal );
										
										// 2.2. refresh RFIDSummary table
										$("#RFIDSummary tbody").text("");
										$.each(result.clothTypeCountList, function(key, value) {
											
											var row = $("<tr/>");
											row.append($("<td/>").text(value.type));
											row.append($("<td/>").text(value.num));
											
											$("#RFIDSummary tbody").append(row);
										});
										
										
									}
									
									if (result.actionMessages != undefined)
									{
										$.each(result.actionMessages, function(key, value) {
											addActionMessage(elementId, value);
										});
									}
									if (result.actionErrors != undefined) {
										$.each(result.actionErrors, function(key, value) {
											addActionError(elementId, value);
										});
									}
									if (result.fieldErrors != undefined){
										$.each(result.fieldErrors, function(key, value) {
											$('input[name="'+key+'"]').addClass("fieldErrors");
										});
									}
								}, "json").error(
								function(jqXHR, textStatus, errorThrown) {
									$('<div class="actionError">' + textStatus + '</div>')
											.appendTo('div.actionErrors[ref="' + elementId + '"]');
								});
						
						/////////////////////////////////////////////////////////////////////////
						// 3. Remove row from HTML and enable the checkbox in rest rows
						/////////////////////////////////////////////////////////////////////////
						$(".rfidMaybeRemoveMarker").each(function() {
							if ($(this).attr("checked"))
							{
								var curRowIndex = $(this).attr("id");
								$("#RFIDTable_row_" + curRowIndex).remove();
							}
							else
							{
								var curRowIndex = $(this).attr("id");
								$(this).attr("disabled", false);
								$("#hidden_rfid_" + curRowIndex).attr("disabled", false);
							}
						});
						
						$("#checkAllDataRows").attr("checked", false);
						
						return false;
					},
					
					"<s:text name="btn.no"/>",
					function()
					{
					}
				);
				
				return false;
			});
	// To server: delete selected cloth from rfidFromMapRfidClothAll
	
	
	
	// Reset Receipt
	<s:url var="resetReceiptUrl" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="resetReceipt"></s:url>
	$("#ajaxBtnReceiptReset").on("click", function() {
		
		optionsAvailableMsgDialog(
				"", 
				"<s:text name="dialog.msg.confirm.ask"/>", 
				
				"<s:text name="btn.yes"/>",
				function()
				{
					$.ajax({
						url: '<s:property value="#resetReceiptUrl"/>',
						data: { "kioskName": hiddenKioskName, "struts.enableJSONValidation": true },
						success: function()
						{
							$("#receiptInfoFieldset .actionErrors").empty();
							$("#receiptInfoFieldset .actionMessages").empty();
							$("#receiptInfoFieldset .actionErrors").hide();
							$("#receiptInfoFieldset .actionMessages").hide();
							
							$("#rfidCaptureDetailFieldset .actionErrors").empty();
							$("#rfidCaptureDetailFieldset .actionMessages").empty();
							$("#rfidCaptureDetailFieldset .actionErrors").hide();
							$("#rfidCaptureDetailFieldset .actionMessages").hide();
							
							$("#staffCodeHandledBy").val("");
							$("#staffCardNumberHandledBy").val("");
							$("#staffCodePickedBy").val("");
							$("#staffCardNumberPickedBy").val("");
							$("#receiptRemark").val("");
							$("#RFIDTotal tbody tr td").text( 0 );
							$("#RFIDSummary tbody").text("");
							$("#RFIDTable tbody").text("");
						},
						complete: function()
						{
							$("#staffCardNumberHandledBy").focus();
						}
					});
				},
				
				"<s:text name="btn.no"/>",
				function()
				{
					// nothing to do
				}
		);
		
		return false;
	});
	
	
	//////////////////////////////////////////////////
	// Go to new page when save success
	//////////////////////////////////////////////////
	<s:url var="getNewPageUrl" namespace="/kiosk" action="cloth-distribute-fixed-reader" method="getMainPage"/>
	$("body").delegate("#addFrom", "addFrom.success", function(e, result, result2) {
		
		msgDialog("", result.actionMessages, "<s:text name="btn.ok"/>",
				
				function()
				{
					var url = "<s:property value="#getNewPageUrl"/>";
					url += "?kioskName=" + hiddenKioskName;
					$(location).attr('href', url);
				}
			);
	});
	
});
	
</script>