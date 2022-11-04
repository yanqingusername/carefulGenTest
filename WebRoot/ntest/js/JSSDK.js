
$(document).ready(function() {
	var currurl = location.href.split('#')[0];
	//ajax注入权限验证    
	$.ajax({
		type : 'POST', //请求方式
		url : "../api/JSSDKHELP.hn",
		dataType : 'json',
		data : {
			"url" : currurl
		},
		complete : function(XMLHttpRequest, textStatus) {},
		error : function(XMLHttpRequest, textStatus, errorThrown) {
			//alert("发生错误：" + errorThrown+"XMLHttpRequeststatus:"+XMLHttpRequest.status+"XMLHttpRequest.readyState:"+XMLHttpRequest.readyState+"textStatus:"+textStatus);  
		},
		success : function(res) {
			var appId = res.appId;
			var nonceStr = res.nonceStr;
			var jsapi_ticket = res.jsapi_ticket;
			var timestamp = res.timestamp;
			var signature = res.signature;
			/*            alert("appid:"+appId+";nonceStr:"+nonceStr+";jsapi_ticket:"+jsapi_ticket+";timestamp:"+timestamp
			        	    +";signature"+signature);*/
			//alert(currurl+"&apothecaryId="+user_id);
			//alert("appId:"+appId +"------- " +"nonceStr:"+ nonceStr +" -----"+"jsapi_ticket:"+jsapi_ticket+"------ "+"timestamp:"+timestamp+"------- "+"signature:"+signature);  
			wx.config({
				debug : false, //开启调试模式,调用的所有api的返回值会在客户端alert出来，若要查看传入的参数，可以在pc端打开，参数信息会通过log打出，仅在pc端时才会打印。    
				appId : appId, //必填，公众号的唯一标识    
				timestamp : timestamp, // 必填，生成签名的时间戳    
				nonceStr : nonceStr, //必填，生成签名的随机串    
				signature : signature, // 必填，签名，见附录1    
				jsApiList : [ 'scanQRCode' ] //必填，需要使用的JS接口列表，所有JS接口列表 见附录2     
			}); // end wx.config  


			wx.ready(function() {
				document.querySelector('#scanQRCode').onclick = function() {
					wx.scanQRCode({
						needResult : 1,
						desc : 'scanQRCode desc',
						success : function(res) {
							//扫码后获取结果参数赋值给Input
							var url = res.resultStr;
							//url(条形码类型,条形码号),部分机型为(条形码号)
							console.log(url);
							var tempArray = url.split(',');
							if (tempArray.length == 1) {
								var barCode = tempArray[0];
								var sample_id = barCode.replace(/\s/g, '').replace(/(\w{4})(?=\w)/g, "$1 ");
								if (sample_id.length > 14) {
									sample_id = sample_id.substring(0, 14);
									$("#res-btn").prop("disabled", false);
								} else if (sample_id.length == 14) {
									$("#res-btn").prop("disabled", false);
								} else {
									$("#res-btn").prop("disabled", true);
								}
								document.querySelector('#sample_id').value = sample_id;
							} else if (tempArray.length == 2) {
								var scanType = tempArray[0];
								var barCode = tempArray[1];
								var sample_id = barCode.replace(/\s/g, '').replace(/(\w{4})(?=\w)/g, "$1 ");
								if (sample_id.length > 14) {
									sample_id = sample_id.substring(0, 14);
									$("#res-btn").prop("disabled", false);
								} else if (sample_id.length == 14) {
									$("#res-btn").prop("disabled", false);
								} else {
									$("#res-btn").prop("disabled", true);
								}
								if (scanType == 'CODE_128') {
									document.querySelector('#sample_id').value = sample_id;
								} else {
									alert("请对准条形码扫码!");
								}
							} else {
								alert("请对准条形码扫码!");
							}
						}
					});
				};
			}); // end ready

			wx.error(function(res) {
				alert("调用微信jsapi返回的状态:" + res.errMsg);
			});
		} // end success  
	}); // end ajax  

}); // end document  