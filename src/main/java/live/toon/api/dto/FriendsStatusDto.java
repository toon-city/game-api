package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FriendsStatusDto {
    private List<FriendDto> friends;
    private List<FriendRequestDto> sentRequests;
    private List<FriendRequestDto> receivedRequests;
    private List<BlockedUserDto> blocked;
}
