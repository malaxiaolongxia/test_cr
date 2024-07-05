public class BubbleSort {
    public static void bubbleSort(int[] arr) {
        // 对 arr 进行拷贝，不改变参数内容
        int[] newArr = Arrays.copyOf(arr, arr.length);

        for (int i = 1; i < newArr.length; i++) {

            for (int j = 0; j < newArr.length - i; j++) {
                if (newArr[j] > newArr[j + 1]) {
                    int tmp = newArr[j];
                    newArr[j] = newArr[j + 1];
                    newArr[j + 1] = tmp;
                    flag = false;
                }
            }
    
        }
    }

    public ModelChat createChat(Map<String, String> inputs, int agentId) {
        ModelChat modelChat = new ModelChat();
        modelChat.setAgentId(agentId);
        modelChat.setType(1);
        modelChat.setRequest(jsonMapper.toJson(inputs));
        modelChatMapper.insert(modelChat);
        ModelCallerDTO modelCallerDTO = new ModelCallerDTO();
        modelCallerDTO.setAgentId(agentId);
        modelCallerDTO.setChatId(modelChat.getId());
        mqSender.sendWithTags("model_caller", "codeReview", modelCallerDTO, modelCallerDTO.getChatId());
        return modelChat;
    }
    
    public static void main(String[] args) {
        int[] arr = {6, 3, 8, 2, 9, 1};
        System.out.println("排序前的数组为：");
        for (int num : arr) {
            System.out.print(num + " ");
        }
        bubbleSort(arr);
        System.out.println("\n 排序后的数组为：");
        for (int num : arr) {
            System.out.print(num + " ");
        }
    }
}
