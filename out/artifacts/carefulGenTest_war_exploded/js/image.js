wx.ready(function() {

	// 5 图片接口
	// 5.1 拍照、本地选图
	var images = {
		localId : [],
		serverId : []
	};
	document.querySelector('#chooseImage').onclick = function() {
		/*
		 * wx.chooseImage({ success: function (res) { images.localId =
		 * res.localIds; alert('已选择 ' + res.localIds.length + ' 张图片'); } });
		 */
		wx.chooseImage({
			count : 1, // 最多能选择多少张图片，默认9
			sizeType : [ 'original', 'compressed' ], // 可以指定是原图还是压缩图，默认二者都有
			sourceType : [ 'album', 'camera' ], // 可以指定来源是相册还是相机，默认二者都有
			success : function(res) {
				var localIds = res.localIds; // 返回选定照片的本地ID列表，localId可以作为img标签的src属性显示图片

				for (var i = 0; i < localIds.length; i++) {
					// 上传图片接口
					wx.uploadImage({
						localId : localIds[i].toString(), // 需要上传的图片的本地ID，由chooseImage接口获得
						isShowProgressTips : 1, // 默认为1，显示进度提示
						success : function(res) {
							var serverId = res.serverId; // 返回图片的服务器端ID
							$.ajax({
								url : '/pretermLaborTest/sys/TestDataController/getPicture.hn?serverId=' + serverId,
								type : 'get',
								dataType : "json",
								success : function(data) {
                                    if(data.success){
                        				var testTime = data.testTime;
                        				var turbidity = data.turbidity;
                        				var advise = data.advise;
                        				window.location.href = "../prematureTest/testdatadetail.jsp?testTime="+ testTime + "&turbidity=" + turbidity+ "&advise=" + advise;
                                    }else{
                                    	alert("图片无法识别");
                                    }
								}
							});
						}
					});
				}
			}
		});
	};
});