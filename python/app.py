from flask import Flask, request, jsonify
from flask_cors import CORS
import redis
import json
import base64
import logging
from typing import Dict, Any
import os

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Redis连接
redis_host = os.environ.get('REDIS_HOST', 'localhost')
redis_port = int(os.environ.get('REDIS_PORT', 6379))
redis_client = redis.Redis(host=redis_host, port=redis_port, decode_responses=True)

# 模拟AI模型（实际项目中应加载真实的TensorFlow/PyTorch模型）
class AIModelService:
    """AI模型服务类"""

    def __init__(self):
        self.models = {}
        logger.info("AI模型服务初始化完成")

    def analyze_ripeness(self, image_data: bytes, crop_type: str) -> Dict[str, Any]:
        """
        果蔬成熟度检测
        Args:
            image_data: 图像字节数据
            crop_type: 作物类型
        Returns:
            分析结果字典
        """
        try:
            # 模拟分析结果
            confidence = 0.85 + (hash(image_data) % 100) / 1000
            ripeness_level = "成熟" if confidence > 0.90 else "未成熟" if confidence < 0.85 else "半成熟"

            result = {
                "success": True,
                "crop_type": crop_type,
                "ripeness_level": ripeness_level,
                "confidence": round(confidence, 4),
                "details": {
                    "color_score": round(0.7 + (hash(image_data) % 30) / 100, 2),
                    "size_score": round(0.8 + (hash(image_data) % 20) / 100, 2),
                    "texture_score": round(0.75 + (hash(image_data) % 25) / 100, 2)
                },
                "suggestion": self._get_ripeness_suggestion(crop_type, ripeness_level)
            }

            # 缓存结果
            cache_key = f"ripeness:{crop_type}:{hash(image_data)}"
            redis_client.setex(cache_key, 3600, json.dumps(result))
            logger.info(f"成熟度检测完成: {crop_type} - {ripeness_level}")

            return result
        except Exception as e:
            logger.error(f"成熟度检测失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def analyze_disease(self, image_data: bytes, crop_type: str) -> Dict[str, Any]:
        """
        病虫害检测
        Args:
            image_data: 图像字节数据
            crop_type: 作物类型
        Returns:
            分析结果字典
        """
        try:
            # 模拟检测结果
            disease_types = {
                "tomato": ["晚疫病", "早疫病", "黄化曲叶病毒病", "健康"],
                "corn": ["玉米大斑病", "玉米小斑病", "玉米锈病", "健康"],
                "rice": ["稻瘟病", "纹枯病", "白叶枯病", "健康"],
                "strawberry": ["灰霉病", "白粉病", "炭疽病", "健康"],
                "cucumber": ["霜霉病", "白粉病", "细菌性角斑病", "健康"],
                "pepper": ["疫病", "炭疽病", "病毒病", "健康"]
            }

            disease_list = disease_types.get(crop_type.lower(), ["健康"])
            detected_disease = disease_list[hash(image_data) % len(disease_list)]
            confidence = 0.75 + (hash(image_data) % 200) / 1000

            result = {
                "success": True,
                "crop_type": crop_type,
                "disease_detected": detected_disease != "健康",
                "disease_name": detected_disease if detected_disease != "健康" else "无",
                "confidence": round(confidence, 4),
                "severity": self._get_disease_severity(confidence, detected_disease),
                "suggestion": self._get_disease_suggestion(crop_type, detected_disease)
            }

            # 缓存结果
            cache_key = f"disease:{crop_type}:{hash(image_data)}"
            redis_client.setex(cache_key, 3600, json.dumps(result))
            logger.info(f"病虫害检测完成: {crop_type} - {detected_disease}")

            return result
        except Exception as e:
            logger.error(f"病虫害检测失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def analyze_dual(self, image_data: bytes, crop_type: str) -> Dict[str, Any]:
        """
        双检一体化分析（成熟度 + 病虫害）
        Args:
            image_data: 图像字节数据
            crop_type: 作物类型
        Returns:
            综合分析结果字典
        """
        try:
            ripeness_result = self.analyze_ripeness(image_data, crop_type)
            disease_result = self.analyze_disease(image_data, crop_type)

            overall_score = (
                (ripeness_result.get("confidence", 0) * 0.4) +
                (disease_result.get("confidence", 0) * 0.6)
            )

            result = {
                "success": True,
                "crop_type": crop_type,
                "ripeness_analysis": ripeness_result,
                "disease_analysis": disease_result,
                "overall_score": round(overall_score, 4),
                "overall_status": self._get_overall_status(ripeness_result, disease_result),
                "recommendation": self._get_comprehensive_recommendation(
                    ripeness_result, disease_result
                )
            }

            logger.info(f"双检分析完成: {crop_type}")
            return result
        except Exception as e:
            logger.error(f"双检分析失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def get_ai_suggestion(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        获取AI智能建议
        Args:
            request_data: 请求参数
        Returns:
            AI建议
        """
        try:
            crop_type = request_data.get("cropType", "unknown")
            temperature = float(request_data.get("temperature", 25))
            humidity = float(request_data.get("humidity", 60))
            ph = float(request_data.get("ph", 6.5))
            growth_stage = request_data.get("growthStage", "vegetative")
            problem_desc = request_data.get("problemDescription", "")

            # 生成智能建议
            suggestion = self._generate_smart_suggestion(
                crop_type, temperature, humidity, ph, growth_stage, problem_desc
            )

            result = {
                "success": True,
                "crop_type": crop_type,
                "environmental_analysis": {
                    "temperature_status": self._analyze_temperature(crop_type, temperature),
                    "humidity_status": self._analyze_humidity(crop_type, humidity),
                    "ph_status": self._analyze_ph(crop_type, ph)
                },
                "growth_suggestion": suggestion["growth"],
                "problem_solution": suggestion["problem"] if problem_desc else "暂无明显问题",
                "preventive_measures": suggestion["preventive"],
                "priority_actions": suggestion["priority"]
            }

            logger.info(f"AI建议生成完成: {crop_type}")
            return result
        except Exception as e:
            logger.error(f"生成AI建议失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def _get_ripeness_suggestion(self, crop_type: str, ripeness_level: str) -> str:
        """获取成熟度建议"""
        suggestions = {
            "成熟": f"{crop_type}已达到最佳采摘期，建议及时采摘以保证品质。",
            "半成熟": f"{crop_type}接近成熟，建议2-3天内采摘，可适当延长货架期。",
            "未成熟": f"{crop_type}尚未成熟，建议继续观察，预计7-10天后可采摘。"
        }
        return suggestions.get(ripeness_level, "建议根据实际情况进行采摘")

    def _get_disease_suggestion(self, crop_type: str, disease: str) -> str:
        """获取病虫害防治建议"""
        if disease == "健康":
            return "当前作物生长状况良好，建议继续保持现有管理方式，定期监测。"

        treatments = {
            "晚疫病": "建议使用甲霜灵锰锌或烯酰吗啉喷雾防治，注意通风排湿。",
            "早疫病": "建议使用代森锰锌或多菌灵喷雾，清除病叶病株。",
            "霜霉病": "建议使用霜霉威盐酸盐或甲霜灵喷雾，加强田间通风。",
            "白粉病": "建议使用三唑类杀菌剂喷雾，避免氮肥过量。",
            "灰霉病": "建议使用异菌脲或嘧霉胺喷雾，控制湿度。"
        }
        return treatments.get(disease, f"建议咨询农业专家针对{disease}制定防治方案")

    def _get_disease_severity(self, confidence: float, disease: str) -> str:
        """获取病害严重程度"""
        if disease == "健康":
            return "无"
        if confidence > 0.9:
            return "严重"
        elif confidence > 0.8:
            return "中度"
        else:
            return "轻微"

    def _get_overall_status(self, ripeness: Dict, disease: Dict) -> str:
        """获取整体状态"""
        if disease.get("disease_detected"):
            return "需要处理病虫害"
        if ripeness.get("ripeness_level") == "成熟":
            return "适合采摘"
        return "继续观察"

    def _get_comprehensive_recommendation(self, ripeness: Dict, disease: Dict) -> str:
        """获取综合建议"""
        if disease.get("disease_detected"):
            return f"首要任务：{disease.get('suggestion')} 同时关注：{ripeness.get('suggestion')}"
        return f"{ripeness.get('suggestion')} 当前作物健康状况良好。"

    def _generate_smart_suggestion(self, crop_type: str, temp: float, humidity: float,
                                   ph: float, stage: str, problem: str) -> Dict[str, str]:
        """生成智能建议"""
        growth_advice = f"{crop_type}当前处于{stage}阶段，"
        problem_advice = ""
        preventive_advice = ""
        priority_actions = []

        # 环境分析
        if temp < 15:
            growth_advice += "温度偏低，建议采取保温措施。"
            priority_actions.append("提高温度至适宜范围")
        elif temp > 35:
            growth_advice += "温度偏高，注意通风降温。"
            priority_actions.append("加强通风降温")
        else:
            growth_advice += "温度适宜。"

        if humidity < 40:
            growth_advice += "湿度偏低，建议适时灌溉。"
            priority_actions.append("补充水分")
        elif humidity > 80:
            growth_advice += "湿度过高，注意通风防病。"
            priority_actions.append("降低湿度预防病害")

        if ph < 5.5:
            growth_advice += "土壤偏酸，建议施用石灰改良。"
        elif ph > 7.5:
            growth_advice += "土壤偏碱，建议施用硫磺改良。"

        # 针对生长阶段的建议
        stage_advice = {
            "seedling": "幼苗期重点注意保温保湿，避免移栽伤根。",
            "vegetative": "营养生长期适时追肥，促进叶片生长。",
            "flowering": "开花期避免水分过多，防止落花。",
            "fruiting": "结果期增加磷钾肥，促进果实发育。",
            "harvest": "收获前减少施肥，确保农产品品质。"
        }

        preventive_advice = stage_advice.get(stage, "建议根据生长阶段调整管理措施")

        # 针对问题的建议
        if problem:
            problem_advice = f"针对问题描述：{problem}，建议加强监测，如问题持续可咨询农技专家。"
            priority_actions.append("持续观察问题描述情况")

        if not priority_actions:
            priority_actions.append("保持当前管理方式")

        return {
            "growth": growth_advice,
            "problem": problem_advice,
            "preventive": preventive_advice,
            "priority": priority_actions
        }

    def _analyze_temperature(self, crop_type: str, temp: float) -> str:
        """分析温度状况"""
        temp_ranges = {
            "tomato": (20, 30),
            "corn": (25, 32),
            "rice": (25, 35),
            "strawberry": (18, 28),
            "cucumber": (20, 30),
            "pepper": (22, 30)
        }
        min_t, max_t = temp_ranges.get(crop_type.lower(), (20, 30))
        if temp < min_t:
            return f"偏低（适宜范围：{min_t}-{max_t}℃）"
        elif temp > max_t:
            return f"偏高（适宜范围：{min_t}-{max_t}℃）"
        return "适宜"

    def _analyze_humidity(self, crop_type: str, humidity: float) -> str:
        """分析湿度状况"""
        if humidity < 40:
            return "偏低（适宜范围：50%-70%）"
        elif humidity > 80:
            return "偏高（适宜范围：50%-70%）"
        return "适宜"

    def _analyze_ph(self, crop_type: str, ph: float) -> str:
        """分析pH值状况"""
        if ph < 5.5:
            return "偏酸（适宜范围：5.5-7.5）"
        elif ph > 7.5:
            return "偏碱（适宜范围：5.5-7.5）")
        return "适宜"


class PredictionService:
    """产量预测服务类"""

    def __init__(self):
        self.models = {}
        self.prediction_history = []
        logger.info("产量预测服务初始化完成")

    def predict_yield(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        预测作物产量
        Args:
            request_data: 请求参数
        Returns:
            预测结果
        """
        try:
            crop_type = request_data.get("cropType", "unknown")
            planting_area = float(request_data.get("plantingArea", 1.0))
            average_yield = float(request_data.get("averageYield", 5000))
            fertilizer_usage = float(request_data.get("fertilizerUsage", 100))
            irrigation_amount = float(request_data.get("irrigationAmount", 500))
            temperature = float(request_data.get("temperature", 25))
            humidity = float(request_data.get("humidity", 60))
            growth_stage = request_data.get("growthStage", "vegetative")

            # 模拟产量预测（实际项目中应使用机器学习模型）
            # 基于历史数据和当前环境因素进行预测
            base_yield = average_yield

            # 温度影响因子
            temp_factor = self._calculate_temp_factor(crop_type, temperature)

            # 湿度影响因子
            humidity_factor = self._calculate_humidity_factor(crop_type, humidity)

            # 肥料影响因子
            fertilizer_factor = self._calculate_fertilizer_factor(fertilizer_usage)

            # 灌溉影响因子
            irrigation_factor = self._calculate_irrigation_factor(irrigation_amount)

            # 生长阶段影响因子
            stage_factor = self._calculate_stage_factor(growth_stage)

            # 综合预测产量
            predicted_yield_per_mu = base_yield * temp_factor * humidity_factor * fertilizer_factor * irrigation_factor * stage_factor
            total_predicted_yield = predicted_yield_per_mu * planting_area

            # 计算置信度
            confidence = 0.75 + (hash(str(request_data)) % 200) / 1000

            result = {
                "success": True,
                "cropType": crop_type,
                "plantingArea": planting_area,
                "predictedYieldPerMu": round(predicted_yield_per_mu, 2),
                "totalPredictedYield": round(total_predicted_yield, 2),
                "unit": "公斤",
                "confidence": round(confidence, 4),
                "factors": {
                    "temperature": {"value": temperature, "factor": round(temp_factor, 4), "status": self._get_factor_status(temp_factor)},
                    "humidity": {"value": humidity, "factor": round(humidity_factor, 4), "status": self._get_factor_status(humidity_factor)},
                    "fertilizer": {"value": fertilizer_usage, "factor": round(fertilizer_factor, 4), "status": self._get_factor_status(fertilizer_factor)},
                    "irrigation": {"value": irrigation_amount, "factor": round(irrigation_factor, 4), "status": self._get_factor_status(irrigation_factor)},
                    "growthStage": {"value": growth_stage, "factor": round(stage_factor, 4)}
                },
                "recommendation": self._get_yield_recommendation(crop_type, predicted_yield_per_mu, total_predicted_yield)
            }

            # 保存到历史记录
            self._save_prediction_history(result)

            logger.info(f"产量预测完成: {crop_type} - {total_predicted_yield}公斤")
            return result
        except Exception as e:
            logger.error(f"产量预测失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def get_prediction_history(self, crop_type: str = None, start_date: str = None, end_date: str = None) -> Dict[str, Any]:
        """获取历史预测数据"""
        try:
            history = self.prediction_history

            # 按作物类型过滤
            if crop_type:
                history = [h for h in history if h.get("cropType") == crop_type]

            # 按日期过滤（模拟）
            if start_date or end_date:
                # 实际项目中应根据真实日期过滤
                pass

            return {
                "success": True,
                "total": len(history),
                "data": history[-10:]  # 返回最近10条记录
            }
        except Exception as e:
            logger.error(f"获取历史数据失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def get_model_info(self) -> Dict[str, Any]:
        """获取预测模型信息"""
        return {
            "success": True,
            "models": {
                "tomato": {
                    "name": "番茄产量预测模型",
                    "version": "1.0.0",
                    "accuracy": 0.85,
                    "lastTrained": "2024-01-15",
                    "features": ["温度", "湿度", "肥料用量", "灌溉量", "生长阶段"]
                },
                "corn": {
                    "name": "玉米产量预测模型",
                    "version": "1.0.0",
                    "accuracy": 0.88,
                    "lastTrained": "2024-01-15",
                    "features": ["温度", "湿度", "肥料用量", "灌溉量", "生长阶段"]
                },
                "rice": {
                    "name": "水稻产量预测模型",
                    "version": "1.0.0",
                    "accuracy": 0.90,
                    "lastTrained": "2024-01-15",
                    "features": ["温度", "湿度", "肥料用量", "灌溉量", "生长阶段"]
                },
                "strawberry": {
                    "name": "草莓产量预测模型",
                    "version": "1.0.0",
                    "accuracy": 0.82,
                    "lastTrained": "2024-01-15",
                    "features": ["温度", "湿度", "肥料用量", "灌溉量", "生长阶段"]
                },
                "cucumber": {
                    "name": "黄瓜产量预测模型",
                    "version": "1.0.0",
                    "accuracy": 0.84,
                    "lastTrained": "2024-01-15",
                    "features": ["温度", "湿度", "肥料用量", "灌溉量", "生长阶段"]
                },
                "pepper": {
                    "name": "辣椒产量预测模型",
                    "version": "1.0.0",
                    "accuracy": 0.83,
                    "lastTrained": "2024-01-15",
                    "features": ["温度", "湿度", "肥料用量", "灌溉量", "生长阶段"]
                }
            }
        }

    def train_model(self) -> Dict[str, Any]:
        """训练预测模型（模拟）"""
        try:
            logger.info("开始训练预测模型...")
            # 实际项目中这里会调用真实的模型训练代码
            logger.info("模型训练完成")
            return {
                "success": True,
                "message": "模型训练成功",
                "timestamp": "2024-01-15T10:30:00Z",
                "accuracy": 0.88
            }
        except Exception as e:
            logger.error(f"模型训练失败: {str(e)}")
            return {"success": False, "error": str(e)}

    def _calculate_temp_factor(self, crop_type: str, temp: float) -> float:
        """计算温度影响因子"""
        optimal_temps = {
            "tomato": (22, 28),
            "corn": (25, 32),
            "rice": (25, 35),
            "strawberry": (18, 25),
            "cucumber": (22, 30),
            "pepper": (23, 28)
        }
        min_t, max_t = optimal_temps.get(crop_type.lower(), (20, 30))
        if min_t <= temp <= max_t:
            return 1.0
        elif temp < min_t:
            return 0.7 + (temp / min_t) * 0.3
        else:
            return 0.7 + (max_t / temp) * 0.3

    def _calculate_humidity_factor(self, crop_type: str, humidity: float) -> float:
        """计算湿度影响因子"""
        optimal_humidity = (50, 70)
        if optimal_humidity[0] <= humidity <= optimal_humidity[1]:
            return 1.0
        elif humidity < optimal_humidity[0]:
            return 0.6 + (humidity / optimal_humidity[0]) * 0.4
        else:
            return 0.6 + (optimal_humidity[1] / humidity) * 0.4

    def _calculate_fertilizer_factor(self, fertilizer: float) -> float:
        """计算肥料影响因子"""
        if fertilizer == 0:
            return 0.5
        elif fertilizer < 50:
            return 0.7
        elif fertilizer < 150:
            return 1.0
        else:
            return 0.9  # 过量

    def _calculate_irrigation_factor(self, irrigation: float) -> float:
        """计算灌溉影响因子"""
        if irrigation == 0:
            return 0.5
        elif irrigation < 300:
            return 0.8
        elif irrigation < 700:
            return 1.0
        else:
            return 0.85  # 过量

    def _calculate_stage_factor(self, stage: str) -> float:
        """计算生长阶段影响因子"""
        stage_factors = {
            "seedling": 0.3,
            "vegetative": 0.6,
            "flowering": 0.8,
            "fruiting": 1.0,
            "harvest": 1.0
        }
        return stage_factors.get(stage.lower(), 0.6)

    def _get_factor_status(self, factor: float) -> str:
        """获取因子状态"""
        if factor >= 0.95:
            return "优秀"
        elif factor >= 0.8:
            return "良好"
        elif factor >= 0.6:
            return "一般"
        else:
            return "较差"

    def _get_yield_recommendation(self, crop_type: str, yield_per_mu: float, total_yield: float) -> str:
        """获取产量建议"""
        if yield_per_mu > 6000:
            return f"{crop_type}预测产量较高，管理措施得当，建议继续保持当前管理方式。预计总产量约{total_yield:.0f}公斤。"
        elif yield_per_mu > 4000:
            return f"{crop_type}预测产量中等，可通过优化施肥和灌溉管理提高产量。预计总产量约{total_yield:.0f}公斤。"
        else:
            return f"{crop_type}预测产量偏低，建议检查土壤肥力、病虫害情况，并加强管理。预计总产量约{total_yield:.0f}公斤。"

    def _save_prediction_history(self, result: Dict[str, Any]):
        """保存预测历史记录"""
        import time
        record = {
            "timestamp": int(time.time()),
            "cropType": result.get("cropType"),
            "predictedYield": result.get("totalPredictedYield"),
            "confidence": result.get("confidence")
        }
        self.prediction_history.append(record)
        # 保持最近100条记录
        if len(self.prediction_history) > 100:
            self.prediction_history = self.prediction_history[-100:]


# 初始化模型服务
ai_service = AIModelService()
prediction_service = PredictionService()


@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({
        "status": "healthy",
        "service": "AI Analysis Service",
        "version": "1.0.0"
    })


@app.route('/api/analyze/ripeness', methods=['POST'])
def analyze_ripeness():
    """果蔬成熟度检测API"""
    try:
        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({"success": False, "error": "缺少图像数据"}), 400

        # 解码base64图像
        if isinstance(data['image'], str):
            image_bytes = base64.b64decode(data['image'])
        else:
            image_bytes = data['image']

        crop_type = data.get('cropType', data.get('crop_type', 'tomato'))
        result = ai_service.analyze_ripeness(image_bytes, crop_type)

        return jsonify(result)
    except Exception as e:
        logger.error(f"成熟度检测API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/api/analyze/disease', methods=['POST'])
def analyze_disease():
    """病虫害检测API"""
    try:
        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({"success": False, "error": "缺少图像数据"}), 400

        # 解码base64图像
        if isinstance(data['image'], str):
            image_bytes = base64.b64decode(data['image'])
        else:
            image_bytes = data['image']

        crop_type = data.get('cropType', data.get('crop_type', 'tomato'))
        result = ai_service.analyze_disease(image_bytes, crop_type)

        return jsonify(result)
    except Exception as e:
        logger.error(f"病虫害检测API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/api/analyze/dual', methods=['POST'])
def analyze_dual():
    """双检一体化分析API"""
    try:
        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({"success": False, "error": "缺少图像数据"}), 400

        # 解码base64图像
        if isinstance(data['image'], str):
            image_bytes = base64.b64decode(data['image'])
        else:
            image_bytes = data['image']

        crop_type = data.get('cropType', data.get('crop_type', 'tomato'))
        result = ai_service.analyze_dual(image_bytes, crop_type)

        return jsonify(result)
    except Exception as e:
        logger.error(f"双检分析API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/api/suggest', methods=['POST'])
def get_suggestion():
    """AI智能建议API"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({"success": False, "error": "缺少请求数据"}), 400

        result = ai_service.get_ai_suggestion(data)
        return jsonify(result)
    except Exception as e:
        logger.error(f"AI建议API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


# 产量预测API路由
@app.route('/api/predict/yield', methods=['POST'])
def predict_yield():
    """作物产量预测API"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({"success": False, "error": "缺少请求数据"}), 400

        result = prediction_service.predict_yield(data)
        return jsonify(result)
    except Exception as e:
        logger.error(f"产量预测API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/api/predict/history', methods=['GET'])
def get_prediction_history():
    """获取历史预测数据API"""
    try:
        crop_type = request.args.get('cropType')
        start_date = request.args.get('startDate')
        end_date = request.args.get('endDate')

        result = prediction_service.get_prediction_history(crop_type, start_date, end_date)
        return jsonify(result)
    except Exception as e:
        logger.error(f"获取历史数据API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/api/predict/model-info', methods=['GET'])
def get_model_info():
    """获取预测模型信息API"""
    try:
        result = prediction_service.get_model_info()
        return jsonify(result)
    except Exception as e:
        logger.error(f"获取模型信息API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/api/predict/train', methods=['POST'])
def train_model():
    """训练预测模型API"""
    try:
        result = prediction_service.train_model()
        return jsonify(result)
    except Exception as e:
        logger.error(f"模型训练API错误: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.errorhandler(404)
def not_found(error):
    return jsonify({"success": False, "error": "接口不存在"}), 404


@app.errorhandler(500)
def internal_error(error):
    return jsonify({"success": False, "error": "服务器内部错误"}), 500


if __name__ == '__main__':
    # 从环境变量获取配置
    host = os.environ.get('HOST', '0.0.0.0')
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('DEBUG', 'false').lower() == 'true'

    logger.info(f"AI服务启动: {host}:{port}")
    app.run(host=host, port=port, debug=debug)
